"""HTTP smoke test against the running MySQL-backed app. Outputs no credentials.

Reads ephemeral .runtime/qa-accounts.json created by local QA and removes it after use.
No database credentials are required; endpoints exercise real JPA persistence.
"""
import base64
import json
import uuid
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / '.runtime'
BASE = 'http://127.0.0.1:8080/api'
passed = []

def check(name, condition):
    if not condition:
        raise AssertionError(name)
    passed.append(name)
    print('PASS: ' + name, flush=True)

def request(path, account=None, method='GET', body=None, content_type='application/json', headers=None):
    outgoing = dict(headers or {})
    if account:
        encoded = base64.b64encode((account['email'] + ':' + account['password']).encode()).decode()
        outgoing['Authorization'] = 'Basic ' + encoded
    if body is not None:
        outgoing['Content-Type'] = content_type
        if not isinstance(body, bytes):
            body = json.dumps(body).encode()
    try:
        with urlopen(Request(BASE+path, data=body, headers=outgoing, method=method), timeout=15) as response:
            raw = response.read()
            return response.status, json.loads(raw) if response.headers.get_content_type() == 'application/json' else raw
    except HTTPError as error:
        return error.code, None

credential_path = RUNTIME / 'qa-accounts.json'
accounts = json.loads(credential_path.read_text())
try:
    student, admin = accounts['student'], accounts['admin']
    check('health confirms database UP', request('/health')[1]['database'] == 'UP')
    check('student login', request('/auth/login', method='POST', body=student)[1]['role'] == 'STUDENT')
    check('admin login', request('/auth/login', method='POST', body=admin)[1]['role'] == 'ADMIN')
    status, own = request('/incidents', student)
    check('browser-created reports persisted', status == 200 and len(own) == 2)
    first = next(item for item in own if item['evidenceFiles'])
    second = next(item for item in own if not item['evidenceFiles'])
    status, proof = request(f"/incidents/{first['incidentId']}/evidence/{first['evidenceFiles'][0]['id']}", student)
    check('database evidence bytes match fixture', status == 200 and proof == (RUNTIME/'qa-proof.png').read_bytes())
    check('anonymous reports rejected', request('/incidents')[0] == 401)
    check('student status change rejected', request(f"/incidents/{first['incidentId']}/status", student, 'PUT', {'status':'Resolved'})[0] == 403)
    outsider = {'email':'qa.other.'+uuid.uuid4().hex+'@campus.test','password':'Tx!'+uuid.uuid4().hex}
    status, created_user = request('/auth/register', method='POST', body={'name':'Integration Other Student', **outsider, 'role':'ADMIN'})
    check('registration cannot escalate role', status == 201 and created_user['role'] == 'STUDENT')
    report = {'incidentType':'Phishing','title':'Safe cross-student integration report','description':'Safe second student report for shared-domain graph verification.', 'suspiciousUrl':'https://fakebank.test/login','severity':'High'}
    status, other = request('/incidents', outsider, 'POST', report)
    check('second student creates real incident', status == 201)
    other_id = other['incidentId']
    check('cross-student incident access rejected', request(f'/incidents/{other_id}', student)[0] == 404 and request(f"/incidents/{first['incidentId']}", outsider)[0] == 404)
    check('cross-student evidence access rejected', request(f"/incidents/{first['incidentId']}/evidence/{first['evidenceFiles'][0]['id']}", outsider)[0] == 404)
    status, graph = request('/graph', admin)
    ids = {node['id'] for node in graph['nodes']}
    domain_edges = [edge for edge in graph['edges'] if edge['target'] == 'domain:fakebank.test']
    check('admin graph connects multiple students to shared domain', status == 200 and len(domain_edges) >= 3 and 'domain:fakebank.test' in ids)
    check('all graph edges reference real unique nodes', len(ids) == len(graph['nodes']) and all(edge['source'] in ids and edge['target'] in ids for edge in graph['edges']))
    status, limited = request('/graph', student)
    check('student graph hides other student incident', status == 200 and f'incident:{other_id}' not in {node['id'] for node in limited['nodes']})
    status, related = request(f"/incidents/{first['incidentId']}/related", admin)
    check('stored relationships include both related reports', status == 200 and {second['incidentId'], other_id}.issubset({item['incidentId'] for item in related}))
    status, updated = request(f"/incidents/{first['incidentId']}/status", admin, 'PUT', {'status':'Resolved'})
    check('admin status update persisted', status == 200 and updated['status'] == 'RESOLVED' and request(f"/incidents/{first['incidentId']}", student)[1]['status'] == 'RESOLVED')
    check('sample DNS service', request('/network/dns?domain=fakebank.test', student)[1]['ipAddress'] == '192.0.2.10')
    check('loopback TCP service', request('/network/tcp?message=TraceX-smoke', student)[1]['receivedMessage'] == 'TraceX-smoke')
    preflight = {'Origin':'http://localhost:5173','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'authorization,content-type'}
    check('frontend CORS preflight succeeds', request('/incidents',method='OPTIONS',headers=preflight)[0] == 200)
    preflight['Origin'] = 'https://unapproved.example'
    check('unapproved CORS origin rejected', request('/incidents',method='OPTIONS',headers=preflight)[0] == 403)
    (RUNTIME/'smoke-results.json').write_text(json.dumps({'passed':len(passed),'failed':0,'checks':passed},indent=2))
    print(f'HTTP smoke checks: {len(passed)} passed, 0 failed. Credentials were not printed.')
finally:
    credential_path.unlink(missing_ok=True)
