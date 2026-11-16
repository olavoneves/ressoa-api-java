// =====================================================================
// Ressoa — MongoDB 01 · database, collections e carga
// Disciplina: Mastering Relational and Non-Relational Database
// Execução: mongosh --file mongodb/01-carga.js   (ou colar no mongosh)
// Os documentos correspondem 1:1 à carga Oracle de 01-dados-teste.sql
// (_id do bairro = BAIRRO.ID; leitura identificada por chaveIdempotencia).
// =====================================================================

// 1) Seleciona (cria) o database
db = db.getSiblingDB("ressoa");

// Reexecutável: remove as collections antes de recriar
db.bairros.drop();
db.leituras.drop();

// 2) Collection "bairros": BAIRRO + AVALIACAO_RISCO e DISPOSITIVO incorporados
db.createCollection("bairros", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["nome", "municipio", "uf", "codigoIbge", "aguaIntermitente", "nivelRisco"],
      properties: {
        uf: { bsonType: "string", minLength: 2, maxLength: 2 },
        codigoIbge: { bsonType: "string", pattern: "^[0-9]{7}$" },
        nivelRisco: { enum: ["VERDE", "AMARELO", "LARANJA", "VERMELHO"] },
        avaliacoes: { bsonType: "array" },
        dispositivos: { bsonType: "array" }
      }
    }
  }
});

// 3) Collection "leituras": série temporal (LEITURA), referência ao aparelho
db.createCollection("leituras", {
  timeseries: { timeField: "registradaEm", metaField: "dispositivo", granularity: "hours" }
});

// 4) Carga
db.bairros.insertMany([
  {
    "_id": 1,
    "nome": "Jardim Ângela",
    "municipio": "São Paulo",
    "uf": "SP",
    "codigoIbge": "3550308",
    "aguaIntermitente": true,
    "nivelRisco": "VERMELHO",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "VERDE",
        "tempMinMedia": 21.5,
        "casos100k": 38.0,
        "rt": 0.92,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "LARANJA",
        "tempMinMedia": 22.4,
        "casos100k": 61.5,
        "rt": 1.04,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "LARANJA",
        "tempMinMedia": 23.1,
        "casos100k": 88.2,
        "rt": 1.18,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "VERMELHO",
        "tempMinMedia": 23.6,
        "casos100k": 112.4,
        "rt": 1.26,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0001",
        "tipoRecipiente": "TAMBOR",
        "volumeLitros": 200,
        "pulsosPorDia": 1,
        "status": "ATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Maria Aparecida"
      },
      {
        "numeroSerie": "RSA-2026-0002",
        "tipoRecipiente": "CAIXA_DAGUA",
        "volumeLitros": 500,
        "pulsosPorDia": 1,
        "status": "ATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Maria Aparecida"
      }
    ]
  },
  {
    "_id": 2,
    "nome": "Grajaú",
    "municipio": "São Paulo",
    "uf": "SP",
    "codigoIbge": "3550308",
    "aguaIntermitente": true,
    "nivelRisco": "LARANJA",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "VERDE",
        "tempMinMedia": 21.0,
        "casos100k": 22.0,
        "rt": 0.85,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "AMARELO",
        "tempMinMedia": 22.1,
        "casos100k": 35.6,
        "rt": 0.97,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "LARANJA",
        "tempMinMedia": 22.8,
        "casos100k": 49.3,
        "rt": 1.09,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "LARANJA",
        "tempMinMedia": 23.2,
        "casos100k": 71.8,
        "rt": 1.15,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0003",
        "tipoRecipiente": "BALDE",
        "volumeLitros": 20,
        "pulsosPorDia": 1,
        "status": "ATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "João Batista"
      }
    ]
  },
  {
    "_id": 3,
    "nome": "Complexo da Maré",
    "municipio": "Rio de Janeiro",
    "uf": "RJ",
    "codigoIbge": "3304557",
    "aguaIntermitente": true,
    "nivelRisco": "VERMELHO",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "LARANJA",
        "tempMinMedia": 23.4,
        "casos100k": 55.1,
        "rt": 1.02,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "LARANJA",
        "tempMinMedia": 24.0,
        "casos100k": 79.9,
        "rt": 1.11,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "VERMELHO",
        "tempMinMedia": 24.6,
        "casos100k": 104.7,
        "rt": 1.22,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "VERMELHO",
        "tempMinMedia": 25.1,
        "casos100k": 131.0,
        "rt": 1.31,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0004",
        "tipoRecipiente": "TAMBOR",
        "volumeLitros": 200,
        "pulsosPorDia": 2,
        "status": "ATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Ana Souza"
      },
      {
        "numeroSerie": "RSA-2026-0005",
        "tipoRecipiente": "BALDE",
        "volumeLitros": 20,
        "pulsosPorDia": 2,
        "status": "BATERIA_BAIXA",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Ana Souza"
      }
    ]
  },
  {
    "_id": 4,
    "nome": "Vila Pinho",
    "municipio": "Belo Horizonte",
    "uf": "MG",
    "codigoIbge": "3106200",
    "aguaIntermitente": true,
    "nivelRisco": "AMARELO",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "VERDE",
        "tempMinMedia": 19.8,
        "casos100k": 12.4,
        "rt": 0.78,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "VERDE",
        "tempMinMedia": 20.6,
        "casos100k": 15.0,
        "rt": 0.83,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "VERDE",
        "tempMinMedia": 21.4,
        "casos100k": 18.9,
        "rt": 0.9,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "AMARELO",
        "tempMinMedia": 22.3,
        "casos100k": 24.6,
        "rt": 0.96,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0006",
        "tipoRecipiente": "CAIXA_DAGUA",
        "volumeLitros": 1000,
        "pulsosPorDia": 1,
        "status": "ATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Carlos Lima"
      }
    ]
  },
  {
    "_id": 5,
    "nome": "Ibura",
    "municipio": "Recife",
    "uf": "PE",
    "codigoIbge": "2611606",
    "aguaIntermitente": true,
    "nivelRisco": "LARANJA",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "AMARELO",
        "tempMinMedia": 24.1,
        "casos100k": 41.2,
        "rt": 0.95,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "AMARELO",
        "tempMinMedia": 24.3,
        "casos100k": 46.8,
        "rt": 0.99,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "LARANJA",
        "tempMinMedia": 24.8,
        "casos100k": 58.3,
        "rt": 1.06,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "LARANJA",
        "tempMinMedia": 25.0,
        "casos100k": 66.0,
        "rt": 1.08,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0007",
        "tipoRecipiente": "TAMBOR",
        "volumeLitros": 200,
        "pulsosPorDia": 1,
        "status": "SEM_SINAL",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Rita Gomes"
      }
    ]
  },
  {
    "_id": 6,
    "nome": "Moema",
    "municipio": "São Paulo",
    "uf": "SP",
    "codigoIbge": "3550308",
    "aguaIntermitente": false,
    "nivelRisco": "VERDE",
    "avaliacoes": [
      {
        "semanaEpidemiologica": 202642,
        "nivel": "VERDE",
        "tempMinMedia": 19.5,
        "casos100k": 4.1,
        "rt": 0.7,
        "criadaEm": ISODate("2026-10-24T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202643,
        "nivel": "VERDE",
        "tempMinMedia": 20.0,
        "casos100k": 5.0,
        "rt": 0.72,
        "criadaEm": ISODate("2026-10-31T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202644,
        "nivel": "VERDE",
        "tempMinMedia": 20.4,
        "casos100k": 6.2,
        "rt": 0.75,
        "criadaEm": ISODate("2026-11-07T18:00:00Z")
      },
      {
        "semanaEpidemiologica": 202645,
        "nivel": "VERDE",
        "tempMinMedia": 21.1,
        "casos100k": 7.8,
        "rt": 0.8,
        "criadaEm": ISODate("2026-11-14T18:00:00Z")
      }
    ],
    "dispositivos": [
      {
        "numeroSerie": "RSA-2026-0008",
        "tipoRecipiente": "BALDE",
        "volumeLitros": 20,
        "pulsosPorDia": 1,
        "status": "INATIVO",
        "instaladoEm": ISODate("2026-10-01T10:00:00Z"),
        "dono": "Lucas Prado"
      }
    ]
  }
]);

db.leituras.insertMany([
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261108",
    "bateriaPct": 92,
    "temperaturaAgua": 24.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261109",
    "bateriaPct": 89,
    "temperaturaAgua": 24.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261110",
    "bateriaPct": 86,
    "temperaturaAgua": 25.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261111",
    "bateriaPct": 83,
    "temperaturaAgua": 24.0,
    "pulsoOk": false
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261112",
    "bateriaPct": 80,
    "temperaturaAgua": 24.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261113",
    "bateriaPct": 77,
    "temperaturaAgua": 25.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0001",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0001-20261114",
    "bateriaPct": 74,
    "temperaturaAgua": 24.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261108",
    "bateriaPct": 85,
    "temperaturaAgua": 25.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261109",
    "bateriaPct": 82,
    "temperaturaAgua": 25.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261110",
    "bateriaPct": 79,
    "temperaturaAgua": 26.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261111",
    "bateriaPct": 76,
    "temperaturaAgua": 25.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261112",
    "bateriaPct": 73,
    "temperaturaAgua": 25.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261113",
    "bateriaPct": 70,
    "temperaturaAgua": 26.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0002",
      "bairroId": 1
    },
    "chaveIdempotencia": "seed-RSA-2026-0002-20261114",
    "bateriaPct": 67,
    "temperaturaAgua": 25.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261108",
    "bateriaPct": 78,
    "temperaturaAgua": 25.5,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261109",
    "bateriaPct": 75,
    "temperaturaAgua": 26.3,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261110",
    "bateriaPct": 72,
    "temperaturaAgua": 27.1,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261111",
    "bateriaPct": 69,
    "temperaturaAgua": 25.5,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261112",
    "bateriaPct": 66,
    "temperaturaAgua": 26.3,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261113",
    "bateriaPct": 63,
    "temperaturaAgua": 27.1,
    "pulsoOk": false
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0003",
      "bairroId": 2
    },
    "chaveIdempotencia": "seed-RSA-2026-0003-20261114",
    "bateriaPct": 60,
    "temperaturaAgua": 25.5,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261108",
    "bateriaPct": 88,
    "temperaturaAgua": 27.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261109",
    "bateriaPct": 85,
    "temperaturaAgua": 27.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261110",
    "bateriaPct": 82,
    "temperaturaAgua": 28.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261111",
    "bateriaPct": 79,
    "temperaturaAgua": 27.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261112",
    "bateriaPct": 76,
    "temperaturaAgua": 27.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261113",
    "bateriaPct": 73,
    "temperaturaAgua": 28.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0004",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0004-20261114",
    "bateriaPct": 70,
    "temperaturaAgua": 27.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261108",
    "bateriaPct": 33,
    "temperaturaAgua": 27.5,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261109",
    "bateriaPct": 30,
    "temperaturaAgua": 28.3,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261110",
    "bateriaPct": 27,
    "temperaturaAgua": 29.1,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261111",
    "bateriaPct": 24,
    "temperaturaAgua": 27.5,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261112",
    "bateriaPct": 21,
    "temperaturaAgua": 28.3,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261113",
    "bateriaPct": 18,
    "temperaturaAgua": 29.1,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0005",
      "bairroId": 3
    },
    "chaveIdempotencia": "seed-RSA-2026-0005-20261114",
    "bateriaPct": 15,
    "temperaturaAgua": 27.5,
    "pulsoOk": false
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261108",
    "bateriaPct": 95,
    "temperaturaAgua": 23.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261109",
    "bateriaPct": 92,
    "temperaturaAgua": 23.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261110",
    "bateriaPct": 89,
    "temperaturaAgua": 24.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-11T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261111",
    "bateriaPct": 86,
    "temperaturaAgua": 23.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-12T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261112",
    "bateriaPct": 83,
    "temperaturaAgua": 23.8,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-13T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261113",
    "bateriaPct": 80,
    "temperaturaAgua": 24.6,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-14T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0006",
      "bairroId": 4
    },
    "chaveIdempotencia": "seed-RSA-2026-0006-20261114",
    "bateriaPct": 77,
    "temperaturaAgua": 23.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-08T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0007",
      "bairroId": 5
    },
    "chaveIdempotencia": "seed-RSA-2026-0007-20261108",
    "bateriaPct": 64,
    "temperaturaAgua": 28.0,
    "pulsoOk": true
  },
  {
    "registradaEm": ISODate("2026-11-09T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0007",
      "bairroId": 5
    },
    "chaveIdempotencia": "seed-RSA-2026-0007-20261109",
    "bateriaPct": 61,
    "temperaturaAgua": 28.8,
    "pulsoOk": false
  },
  {
    "registradaEm": ISODate("2026-11-10T09:00:00Z"),
    "dispositivo": {
      "numeroSerie": "RSA-2026-0007",
      "bairroId": 5
    },
    "chaveIdempotencia": "seed-RSA-2026-0007-20261110",
    "bateriaPct": 58,
    "temperaturaAgua": 29.6,
    "pulsoOk": true
  }
]);

// Índices de apoio aos relatórios
db.bairros.createIndex({ nivelRisco: 1, aguaIntermitente: 1 });
db.bairros.createIndex({ "dispositivos.numeroSerie": 1 }, { unique: true, sparse: true });
db.leituras.createIndex({ "dispositivo.numeroSerie": 1, registradaEm: -1 });

print("bairros: " + db.bairros.countDocuments() + " documentos");
print("leituras: " + db.leituras.countDocuments() + " documentos");
