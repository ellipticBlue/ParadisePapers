# Paradise Papers
App that provides an API for data ingest and shortest path calculations on Neo4J

# Assumptions
Reading through the problem, the code embodies the following assumptions:
1. In path length calculations, all edges are assumed to be of the same weight, which is taken to be 1.
2. Shortest path calculations are undirected, i.e. a valid paths are not ones that follow edge directions consistently forward or backward.
3. All shortest paths hold equal value. Thus, the code returns all paths that are of the shortest length.
4. Code targeted to individual user in a research context. Accuracy is of highest importance with speed of calculation a secondary concern. I.e. the code needs to find all shortest paths in a deterministic (not probabalistic manner)

# Requirements
1. MySQL and Neo4J servers running, with dataset available via the MySQL server (connection parameters can be set in the application.conf)
2. When working with large graphs, such as those in the provided dataset, the JVM heap size may need adjustment. Testing was carried out on the provided dataset using a 10 gigabyte heap.

# API
Running the application will create a REST API on localhost accessible via port 8080. The following commands can be supplied
* /clearGraph - GET request that will clear the graph specified in the application.conf on the Neo4J server
* /ingestData - GET request that will clear the graph specified in the application.conf and initiate a data migration procedure ingesting the data MySQL DB into the Neo4J Graph
* /shortestPath - POST request that takes a JSON input file specifying the IDs (as specified in the original MySQL data) of two nodes in the Graph and returns all the shortest paths (i.e. all have the same length) joining those two nodes. The JSON input has the following format:
```
{
  "startNodeId": 123456,
  "endNodeId": 654321
}
```
Below is an example of return JSON (note each path contains alternating vertex-edge entries with the top of the 
array corresponding to the starting vertex):
```
{
  "paths": [{
    "path": [{
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 2,
        "ibcRUC": "wx",
        "nodeLabel": "entity",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 1,
        "relationType": "foo",
        "nodeOne": 2,
        "nodeTwo": 1
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 1,
        "ibcRUC": "wx",
        "nodeLabel": "address",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 3,
        "relationType": "baz",
        "nodeOne": 4,
        "nodeTwo": 1
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 4,
        "ibcRUC": "wx",
        "nodeLabel": "officer",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 7,
        "relationType": "bar",
        "nodeOne": 4,
        "nodeTwo": 7
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 7,
        "ibcRUC": "wx",
        "nodeLabel": "officer",
        "sourceId": "gh"
      }
    }]
  }, {
    "path": [{
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 2,
        "ibcRUC": "wx",
        "nodeLabel": "entity",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 1,
        "relationType": "foo",
        "nodeOne": 2,
        "nodeTwo": 1
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 1,
        "ibcRUC": "wx",
        "nodeLabel": "address",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 5,
        "relationType": "foo",
        "nodeOne": 1,
        "nodeTwo": 6
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 6,
        "ibcRUC": "wx",
        "nodeLabel": "address",
        "sourceId": "gh"
      },
      "connectingEdge": {
        "index": 8,
        "relationType": "baz",
        "nodeOne": 7,
        "nodeTwo": 6
      }
    }, {
      "vertex": {
        "jurisdictionDescription": "mn",
        "jurisdiction": "qr",
        "name": "kl",
        "validUntil": "bc",
        "serviceProvider": "op",
        "incorporationDate": "uv",
        "countries": "ef",
        "companyType": "cd",
        "closedDate": "st",
        "labels": "ab",
        "note": "ef",
        "countryCode": "cd",
        "status": "ab",
        "nodeType": "yz",
        "address": "ij",
        "nodeId": 7,
        "ibcRUC": "wx",
        "nodeLabel": "officer",
        "sourceId": "gh"
      }
    }]
  }]
```

