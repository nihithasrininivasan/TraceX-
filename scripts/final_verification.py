"""Verify the existing schema read-only, then exercise all APIs in a new MySQL database.
Existing accounts/reports are never modified. Secrets are inherited in memory, not logged.
The verification database is retained for inspection; only its temporary backend is stopped.
"""
import base64
import json
import os
import socket
import subprocess
import time
import uuid
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.runtime'
RUNTIME.mkdir(exist_ok=True)
MYSQL = r'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
JAVA = r'C:\Program Files\Java\jdk-21.0.12\bin\java.exe'
SOURCE_DB = os.environ['TRACEX_VERIFY_SOURCE_DB']
DB_USER = os.environ['TRACEX_VERIFY_DB_USER']
VERIFY_DB = 'tracex_verify_' + uuid.uuid4().hex[:12]
passed, coverage = [], set()
process = None
report_file = RUNTIME / 'final-verification.json'

def check(name, condition):
    if not condition:
        raise AssertionError(name)
    passed.append(name)
    print('PASS: ' + name, flush=True)

def sql(database, query):
    result = subprocess.run([MYSQL, '--host=127.0.0.1', '--port=3306', '--user='+DB_USER,
        '--database='+database, '--connect-timeout=5', '--batch', '--skip-column-names'],
        input=query, text=True, capture_output=True, timeout=20)
    if result.returncode:
        raise RuntimeError('MySQL verification query failed; no credentials are printed.')
    return [line.split('\t') for line in result.stdout.strip().splitlines() if line]

def scalar(database, query):
    return int(sql(database, query)[0][0])

def foreign_keys(database):
    rows = sql(database, "SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL;")
    actual = {tuple(row) for row in rows}
    expected = {('incidents','user_id','users','id'),('evidence','incident_id','incidents','incident_id'),
        ('incident_relationships','source_incident_id','incidents','incident_id'),
        ('incident_relationships','target_incident_id','incidents','incident_id'),('audit_logs','user_id','users','id')}
    check(('existing' if database == SOURCE_DB else 'verification')+' database has all five foreign keys', expected.issubset(actual))
    return rows

with socket.socket() as listener:
    listener.bind(('127.0.0.1', 0))
    PORT = listener.getsockname()[1]
BASE = f'http://127.0.0.1:{PORT}/api'

def request(path, account=None, method='GET', body=None, content_type='application/json', headers=None):
    outgoing = dict(headers or {})
    if account:
        outgoing['Authorization'] = 'Basic '+base64.b64encode((account['email']+':'+account['password']).encode()).decode()
    if body is not None:
        outgoing['Content-Type'] = content_type
        if not isinstance(body, bytes):
            body = json.dumps(body).encode()
    coverage.add(method+' '+path.split('?')[0])
    try:
        with urlopen(Request(BASE+path, data=body, headers=outgoing, method=method), timeout=15) as response:
            raw = response.read()
            return response.status, json.loads(raw) if response.headers.get_content_type() == 'application/json' else raw
    except HTTPError as error:
        return error.code, None

def register(name):
    account = {'email':'verify.'+uuid.uuid4().hex+'@campus.test', 'password':'Tx!'+uuid.uuid4().hex}
    status, user = request('/auth/register', method='POST', body={'name':name, **account, 'role':'ADMIN'})
    check('register '+name+' assigns STUDENT', status == 201 and user['role'] == 'STUDENT')
    return {**account, 'id':user['id']}

def incident(account, url=None, ip=None, title='Final verification report', evidence=False, omit_url=False):
    body = {'incidentType':'Phishing','title':title,'description':'Safe final integration verification with synthetic test data only.',
        'senderEmail':'+91 90000 00000','severity':'Medium'}
    if not omit_url:
        body['suspiciousUrl'] = url
    if ip is not None:
        body['sampleIp'] = ip
    if evidence:
        boundary = 'TraceX'+uuid.uuid4().hex
        raw = (f'--{boundary}\r\nContent-Disposition: form-data; name="incident"\r\nContent-Type: application/json\r\n\r\n'.encode()
            + json.dumps(body).encode()
            + f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="evidence"; filename="proof.png"\r\nContent-Type: image/png\r\n\r\n'.encode()
            + PNG + f'\r\n--{boundary}--\r\n'.encode())
        status, saved = request('/incidents', account, 'POST', raw, 'multipart/form-data; boundary='+boundary)
    else:
        status, saved = request('/incidents', account, 'POST', body)
    check('report saved: '+title, status == 201)
    return saved

PNG = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aFfoAAAAASUVORK5CYII=')
try:
    check('source database name is safe', SOURCE_DB.replace('_','').isalnum())
    source_foreign_keys = foreign_keys(SOURCE_DB)
    check('existing scores are all normalized', scalar(SOURCE_DB, 'SELECT COUNT(*) FROM incident_relationships WHERE matching_score IS NULL OR matching_score<0 OR matching_score>1;') == 0)
    check('existing relationships contain no duplicate pairs/types', scalar(SOURCE_DB, 'SELECT COUNT(*) FROM (SELECT source_incident_id,target_incident_id,relationship_type FROM incident_relationships GROUP BY source_incident_id,target_incident_id,relationship_type HAVING COUNT(*)>1) d;') == 0)
    check('existing creation audit actors match report owners', scalar(SOURCE_DB, "SELECT COUNT(*) FROM audit_logs a JOIN incidents i ON a.action=CONCAT('CREATE_INCIDENT:',i.incident_id) WHERE a.user_id<>i.user_id;") == 0)
    check('existing status audit actors are administrators', scalar(SOURCE_DB, "SELECT COUNT(*) FROM audit_logs a JOIN users u ON a.user_id=u.id WHERE a.action LIKE 'STATUS:%' AND u.role<>'ADMIN';") == 0)
    check('existing data has no orphan evidence or relationships', scalar(SOURCE_DB, 'SELECT (SELECT COUNT(*) FROM evidence e LEFT JOIN incidents i ON e.incident_id=i.incident_id WHERE i.incident_id IS NULL)+(SELECT COUNT(*) FROM incident_relationships r LEFT JOIN incidents a ON r.source_incident_id=a.incident_id LEFT JOIN incidents b ON r.target_incident_id=b.incident_id WHERE a.incident_id IS NULL OR b.incident_id IS NULL);') == 0)
    # The only schema creation is a new, randomly named database; nothing is dropped.
    sql(SOURCE_DB, f'CREATE DATABASE {VERIFY_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
    env = dict(os.environ)
    env.update(DB_URL=f'jdbc:mysql://127.0.0.1:3306/{VERIFY_DB}', DB_USER=DB_USER, DB_PASSWORD=os.environ['MYSQL_PWD'])
    jar = ROOT/'backend/target/traceroute-backend-0.0.1-SNAPSHOT.jar'
    log = open(RUNTIME/'final-verification-backend.log', 'w')
    process = subprocess.Popen([JAVA,'-jar',str(jar),'--server.port='+str(PORT)],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT)
    for attempt in range(60):
        if process.poll() is not None:
            raise RuntimeError('Isolated verification backend stopped during startup.')
        try:
            if request('/health')[0] == 200:
                break
        except (URLError, TimeoutError, OSError):
            pass
        time.sleep(1)
    else:
        raise RuntimeError('Isolated verification backend did not become ready.')
    check('isolated backend uses real MySQL', request('/health')[1]['database'] == 'UP')
    verification_foreign_keys = foreign_keys(VERIFY_DB)
    a, b, admin = register('Verification Student A'), register('Verification Student B'), register('Verification Admin')
    # Explicitly approved fixture admin, scoped to the new verification database.
    sql(VERIFY_DB, f"UPDATE users SET role='ADMIN' WHERE id={admin['id']};")
    for name, account, role in [('student',a,'STUDENT'),('administrator',admin,'ADMIN')]:
        status, user = request('/auth/login',method='POST',body={k:account[k] for k in ['email','password']})
        check(name+' login returns correct role', status == 200 and user['role'] == role)
    check('invalid login rejected', request('/auth/login',method='POST',body={'email':a['email'],'password':'incorrect'})[0] == 401)
    check('duplicate registration rejected', request('/auth/register',method='POST',body={'name':'Duplicate','email':a['email'],'password':a['password']})[0] == 409)
    check('current-user endpoint identifies caller', request('/users/me',a)[1]['id'] == a['id'])
    check('student cannot list all users', request('/users',a)[0] == 403)
    check('administrator can list users', request('/users',admin)[0] == 200)
    check('empty graph is supported', request('/graph',a)[1] == {'nodes':[],'edges':[]})
    for case, kwargs in [('omitted',{'omit_url':True}),('null',{'url':None}),('blank',{'url':''})]:
        saved = incident(a,title='No URL: '+case,**kwargs)
        check('URL '+case+' produces no domain or IP', saved['domain'] is None and saved['sampleIp'] is None)
    first = incident(a,'https://fakebank.test/login',title='Domain and IP first',evidence=True)
    second = incident(b,'https://fakebank.test/login',title='Domain and IP second')
    incident(a,'https://domain-only.test/a',title='Domain-only first')
    incident(a,'https://domain-only.test/b',title='Domain-only second')
    incident(a,'https://ip-one.test/a','198.51.100.7',title='IP-only first')
    incident(a,'https://ip-two.test/a','198.51.100.7',title='IP-only second')
    scores = {float(row[0]) for row in sql(VERIFY_DB,'SELECT DISTINCT matching_score FROM incident_relationships;')}
    check('MySQL stores exactly 0.4, 0.6 and 1.0', scores == {0.4,0.6,1.0})
    status, own = request('/incidents',a)
    check('student list contains only owned incidents', status == 200 and all(item['user']['id'] == a['id'] for item in own))
    check('admin list includes all saved incidents', len(request('/incidents',admin)[1]) == scalar(VERIFY_DB,'SELECT COUNT(*) FROM incidents;'))
    check('incident details are accessible to owner', request(f"/incidents/{first['incidentId']}",a)[0] == 200)
    check('other student cannot access report', request(f"/incidents/{first['incidentId']}",b)[0] == 404)
    check('anonymous cannot access reports', request('/incidents')[0] == 401)
    file = first['evidenceFiles'][0]
    path = f"/incidents/{first['incidentId']}/evidence/{file['id']}"
    check('evidence download bytes match original', request(path,a) == (200,PNG))
    check('admin can download evidence', request(path,admin)[0] == 200)
    check('other student cannot download evidence', request(path,b)[0] == 404)
    check('related reports obey student visibility', request(f"/incidents/{first['incidentId']}/related",a)[1] == [])
    check('admin sees related other-student report', second['incidentId'] in {item['incidentId'] for item in request(f"/incidents/{first['incidentId']}/related",admin)[1]})
    _, graph = request('/graph',admin)
    ids = [node['id'] for node in graph['nodes']]
    edges = graph['edges']
    check('graph node IDs are unique', len(ids) == len(set(ids)))
    check('graph edge IDs are unique', len(edges) == len({edge['id'] for edge in edges}))
    check('graph connections are not duplicated', len(edges) == len({(edge['source'],edge['target']) for edge in edges}))
    check('all graph edge endpoints exist', all(edge['source'] in ids and edge['target'] in ids for edge in edges))
    check('graph API preserves normalized scores', {edge['data']['matchScore'] for edge in edges if 'data' in edge} == scores)
    check('graph aliases return same node IDs', set(ids) == {node['id'] for node in request('/network/graph',admin)[1]['nodes']})
    check('student graph hides another student report', f"incident:{second['incidentId']}" not in {node['id'] for node in request('/graph',a)[1]['nodes']})
    check('relationship API returns normalized scores', {link['matchScore'] for link in request('/relationships',admin)[1]} == scores)
    check('incident relationship API is filtered', all(link['sourceIncidentId']==first['incidentId'] or link['targetIncidentId']==first['incidentId'] for link in request(f"/relationships/incident/{first['incidentId']}",admin)[1]))
    before = scalar(VERIFY_DB,'SELECT COUNT(*) FROM audit_logs;')
    status_path = f"/incidents/{first['incidentId']}/status"
    check('student cannot change status', request(status_path,a,'PUT',{'status':'Resolved'})[0] == 403)
    check('denied action does not create success audit', scalar(VERIFY_DB,'SELECT COUNT(*) FROM audit_logs;') == before)
    check('admin can change status', request(status_path,admin,'PUT',{'status':'Resolved'})[1]['status'] == 'RESOLVED')
    check('status persists in subsequent student request', request(f"/incidents/{first['incidentId']}",a)[1]['status'] == 'RESOLVED')
    check('all creation audits identify authenticated owners', scalar(VERIFY_DB,"SELECT COUNT(*) FROM audit_logs a JOIN incidents i ON a.action=CONCAT('CREATE_INCIDENT:',i.incident_id) WHERE a.user_id<>i.user_id;") == 0)
    actor = scalar(VERIFY_DB,f"SELECT user_id FROM audit_logs WHERE action='STATUS:{first['incidentId']}:RESOLVED';")
    check('status audit identifies authenticated admin', actor == admin['id'])
    check('DNS demonstration works', request('/network/dns?domain=fakebank.test',a)[1]['ipAddress']=='192.0.2.10')
    check('IP results respect ownership', all(item['user']['id']==a['id'] for item in request('/network/ip?address=192.0.2.10',a)[1]))
    check('IP count matches permitted results', request('/network/ip/count?address=192.0.2.10',a)[1]['relatedIncidentCount']==len(request('/network/ip?address=192.0.2.10',a)[1]))
    check('TCP loopback demonstration works', request('/network/tcp?message=TraceX-final',a)[1]['receivedMessage']=='TraceX-final')
    check('frontend origin is allowed', request('/incidents',method='OPTIONS',headers={'Origin':'http://localhost:5173','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'authorization,content-type'})[0]==200)
    report = {'status':'passed','passed':len(passed),'failed':0,'database':VERIFY_DB,'source_database':SOURCE_DB,
        'source_foreign_keys':source_foreign_keys,'verification_foreign_keys':verification_foreign_keys,'checks':passed,'api_calls':sorted(coverage)}
    report_file.write_text(json.dumps(report,indent=2))
    print(f'FINAL: {len(passed)} checks passed; verification database: {VERIFY_DB}. Main TraceX data was not changed.',flush=True)
except Exception as error:
    report_file.write_text(json.dumps({'status':'failed','passed':len(passed),'failed':1,'database':VERIFY_DB,'failure':str(error)},indent=2))
    print('FAILED: '+str(error),flush=True)
    raise SystemExit(1)
finally:
    if process and process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
