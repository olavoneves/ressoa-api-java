// =====================================================================
// Ressoa — MongoDB 02 · Relatórios (equivalentes a 06-relatorios-oracle.sql)
// Execução: mongosh --file mongodb/02-relatorios.js
// =====================================================================
db = db.getSiblingDB("ressoa");

function titulo(texto) {
  print("\n=== " + texto + " ===");
}

// ---------------------------------------------------------------------
// R1 (find) — Alertas da semana 08–14/11: pulso com falha ou bateria < 20%
// Oracle: LEITURA JOIN DISPOSITIVO. Mongo: o número de série já está no
// documento (referência estendida), então não há junção.
// ---------------------------------------------------------------------
titulo("R1 · Leituras com falha no pulso ou bateria abaixo de 20%");
db.leituras.find(
  {
    registradaEm: { $gte: ISODate("2026-11-08T00:00:00Z"), $lt: ISODate("2026-11-15T00:00:00Z") },
    $or: [{ pulsoOk: false }, { bateriaPct: { $lt: 20 } }]
  },
  { _id: 0, "dispositivo.numeroSerie": 1, registradaEm: 1, bateriaPct: 1, pulsoOk: 1 }
).sort({ registradaEm: 1, "dispositivo.numeroSerie": 1 }).forEach(printjson);

// ---------------------------------------------------------------------
// R2 (find) — Bairros com água intermitente em risco LARANJA ou VERMELHO,
// com a avaliação mais recente. Oracle: BAIRRO JOIN AVALIACAO_RISCO com
// subconsulta da última avaliação. Mongo: avaliações incorporadas,
// $slice: -1 devolve só a última.
// ---------------------------------------------------------------------
titulo("R2 · Bairros com água intermitente em risco LARANJA ou VERMELHO");
db.bairros.find(
  { aguaIntermitente: true, nivelRisco: { $in: ["LARANJA", "VERMELHO"] } },
  { _id: 0, nome: 1, municipio: 1, nivelRisco: 1, avaliacoes: { $slice: -1 } }
).sort({ nome: 1 }).forEach(function (b) {
  var a = b.avaliacoes[0];
  printjson({ bairro: b.nome, municipio: b.municipio, nivelRisco: b.nivelRisco,
              semana: a.semanaEpidemiologica, casos100k: a.casos100k, rt: a.rt });
});

// ---------------------------------------------------------------------
// R3 (agregação) — Cobertura de aparelhos por bairro (sem INATIVOS)
// Oracle: GROUP BY sobre BAIRRO JOIN DISPOSITIVO.
// Mongo: $unwind dos aparelhos incorporados + $group.
// ---------------------------------------------------------------------
titulo("R3 · Cobertura de aparelhos ativos por bairro (agregação)");
db.bairros.aggregate([
  { $unwind: "$dispositivos" },
  { $match: { "dispositivos.status": { $ne: "INATIVO" } } },
  { $group: {
      _id: "$nome",
      aparelhos: { $sum: 1 },
      ativos: { $sum: { $cond: [{ $eq: ["$dispositivos.status", "ATIVO"] }, 1, 0] } }
  } },
  { $project: {
      _id: 0, bairro: "$_id", aparelhos: 1, ativos: 1,
      pctAtivos: { $round: [{ $multiply: [{ $divide: ["$ativos", "$aparelhos"] }, 100] }, 1] }
  } },
  { $sort: { pctAtivos: 1, bairro: 1 } }
]).forEach(printjson);

// ---------------------------------------------------------------------
// R4 (agregação, extra) — Saúde da proteção por bairro na semana
// Oracle: 3 tabelas (LEITURA, DISPOSITIVO, BAIRRO).
// Mongo: $group na série temporal + $lookup para o nome do bairro.
// ---------------------------------------------------------------------
titulo("R4 · Taxa de sucesso do pulso e bateria média por bairro (agregação)");
db.leituras.aggregate([
  { $match: { registradaEm: { $gte: ISODate("2026-11-08T00:00:00Z"), $lt: ISODate("2026-11-15T00:00:00Z") } } },
  { $group: {
      _id: "$dispositivo.bairroId",
      leituras: { $sum: 1 },
      pulsosOk: { $sum: { $cond: ["$pulsoOk", 1, 0] } },
      bateriaMedia: { $avg: "$bateriaPct" }
  } },
  { $lookup: { from: "bairros", localField: "_id", foreignField: "_id", as: "bairro" } },
  { $project: {
      _id: 0,
      bairro: { $first: "$bairro.nome" },
      leituras: 1,
      pctPulsoOk: { $round: [{ $multiply: [{ $divide: ["$pulsosOk", "$leituras"] }, 100] }, 1] },
      bateriaMedia: { $round: ["$bateriaMedia", 1] }
  } },
  { $sort: { pctPulsoOk: 1, bairro: 1 } }
]).forEach(printjson);
