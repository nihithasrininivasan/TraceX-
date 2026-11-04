package com.traceroute.networking;

import com.traceroute.incident.service.IncidentService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class GraphController {
    private final IncidentService incidents;
    public GraphController(IncidentService incidents) { this.incidents=incidents; }
    @GetMapping({"/api/graph","/api/network/graph"})
    public Map<String,Object> graph() {
        var nodes=new LinkedHashMap<String,Map<String,Object>>();
        var edges=new ArrayList<Map<String,Object>>();
        for(var incident: incidents.getAllIncidents()) {
            String id="incident:"+incident.incidentId();
            addNode(nodes,id,"incident","Incident #"+incident.incidentId(),incident.title(),incident);
            if(incident.user()!=null) {
                String user="student:"+incident.user().id();
                addNode(nodes,user,"student",incident.user().name(),"Reported incident",null); addEdge(edges,user,id);
            }
            Map<String,String> indicators=new LinkedHashMap<>();
            indicators.put("domain",incident.domain()); indicators.put("url",incident.suspiciousUrl()); indicators.put("ip",incident.sampleIp());
            indicators.forEach((kind,value)-> { if(value!=null && !value.isBlank()) {
                String indicator=kind+":"+value;
                addNode(nodes,indicator,kind,value,"Shared indicators suggest a possible relationship",null); addEdge(edges,id,indicator);
            }});
        }
        for(var link:incidents.visibleRelationships()) {
            var edge=new LinkedHashMap<String,Object>();
            edge.put("id","relationship:"+link.getRelationshipId()); edge.put("source","incident:"+link.getSourceIncidentId());
            edge.put("target","incident:"+link.getTargetIncidentId()); edge.put("label",link.getRelationshipType());
            edge.put("data",Map.of("matchScore",link.getMatchScore(),"matchedIndicator",link.getMatchedIndicator())); edges.add(edge);
        }
        return Map.of("nodes",new ArrayList<>(nodes.values()),"edges",edges);
    }
    private void addNode(Map<String,Map<String,Object>> nodes,String id,String kind,String label,String detail,Object incident) {
        if(nodes.containsKey(id)) return;
        long row=nodes.values().stream().filter(node -> kind.equals(node.get("type"))).count();
        int column=switch(kind) {case "student" -> 0; case "incident" -> 1; case "ip" -> 3; default -> 2;};
        // URL and domain rows share a column, so count both to prevent overlapping nodes.
        if(column==2) row=nodes.values().stream().filter(node -> List.of("url","domain").contains(node.get("type"))).count();
        var data=new LinkedHashMap<String,Object>(); data.put("kind",kind);data.put("label",label);data.put("detail",detail==null?"":detail);
        if(incident!=null)data.put("incident",Map.of("id",String.valueOf(((com.traceroute.incident.dto.IncidentResponse)incident).incidentId())));
        nodes.put(id,Map.of("id",id,"label",label,"type",kind,"position",Map.of("x",column*290,"y",row*130),"data",data));
    }
    private void addEdge(List<Map<String,Object>> edges,String source,String target) {
        edges.add(Map.of("id",source+"->"+target,"source",source,"target",target));
    }
}
