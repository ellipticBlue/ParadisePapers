package com.paradise.model

// Class for internal handling of nodes, as ingested from the RDBMS.
// Note: For Graph nodes we have a separate class that contains an additional 'label' val
case class DBNode(labels: String,
                  validUntil: String,
                  countryCode: String,
                  countries: String,
                  nodeId: Int,
                  sourceId: String,
                  address: String,
                  name: String,
                  jurisdictionDescription: String,
                  serviceProvider: String,
                  jurisdiction: String,
                  closedDate: String,
                  incorporationDate: String,
                  ibcRUC: String,
                  nodeType: String,
                  status: String,
                  companyType: String,
                  note: String)
