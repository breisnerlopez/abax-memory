---
fase: 8-estabilizacion
entregable: benchmark-beir-scifact
responsable: devops-engineer
aprobado-por: tech-lead
fecha: 2026-05-04
estado: completado
version: 1.0
criterio-verificado: CE-01 (NDCG@10 >= 0.80), CE-04 (Recall@10 >= 0.90)
---

# Benchmark BEIR SciFact — Abax-Memory v2.0.0

## Tabla de contenidos

- [Resumen ejecutivo](#resumen-ejecutivo)
- [Metodologia](#metodologia)
- [Configuracion del benchmark](#configuracion-del-benchmark)
- [Resultados](#resultados)
- [Verificacion CE-01 y CE-04](#verificacion-ce-01-y-ce-04)
- [Analisis de brecha (gap analysis)](#analisis-de-brecha-gap-analysis)
- [Limitaciones del benchmark](#limitaciones-del-benchmark)
- [Recomendaciones](#recomendaciones)
- [Evidencia reproducible](#evidencia-reproducible)
- [Glosario](#glosario)

---

## Resumen ejecutivo

Se ejecuto el benchmark **BEIR SciFact** contra un pipeline de busqueda vectorial que replica la configuracion base de Abax-Memory v2.0.0 (OpenAI `text-embedding-3-large` + Qdrant Cosine).

**Veredicto**: CE-01 **NO SE CUMPLE**. NDCG@10 = 0.7771 (umbral requerido: >= 0.80). La brecha es de **-0.0229** (2.9% por debajo del objetivo).

**Veredicto CE-04**: Recall@10 = 0.9006 (umbral: >= 0.90). **PASA por margen minimo** (+0.0006).

---

## Metodologia

### Dataset

| Propiedad | Valor |
|-----------|-------|
| Dataset | BEIR SciFact (test split) |
| Documentos | 5,183 abstracts cientificos |
| Queries | 300 preguntas/claims cientificas |
| Qrels | 300 juicios de relevancia (1 query = 1 conjunto de docs relevantes) |
| Fuente | https://public.ukp.informatik.tu-darmstadt.de/thakur/BEIR/datasets/scifact.zip |

### Pipeline evaluado

```text
Query → OpenAI text-embedding-3-large (3072d) → Qdrant Cosine → Top-10 docs
```

**Nota importante**: Este benchmark evalua exclusivamente el componente de busqueda densa (dense retrieval) del stack Abax-Memory v2.0.0. **No** incluye: busqueda hibrida (BM25 + dense), reranking con cross-encoder, expansion de query, ni filtros semánticos adicionales que pueden estar presentes en el pipeline completo de produccion.

### Protocolo

1. Descarga del dataset BEIR SciFact (test split).
2. Indexacion completa de los 5,183 documentos en una coleccion Qdrant nueva.
3. Embeddings generados con `text-embedding-3-large` (3072 dimensiones).
4. Para cada una de las 300 queries, busqueda de top-10 documentos por similitud coseno.
5. Evaluacion con el modulo `EvaluateRetrieval` de BEIR (k=10).
6. Limpieza de la coleccion al finalizar.

---

## Configuracion del benchmark

| Parametro | Valor |
|-----------|-------|
| Modelo de embedding | `text-embedding-3-large` (OpenAI) |
| Dimensiones | 3072 |
| Vector DB | Qdrant v1.17.1 (Docker) |
| Metrica de distancia | Cosine |
| Indice | HNSW (configuracion por defecto) |
| BATCH_SIZE (indexacion) | 100 documentos |
| K de busqueda | 10 |
| Tokens consumidos (indexacion) | ~1,657,885 |
| Entorno de ejecucion | Linux, Python 3.12, venv aislado |
| Fecha de ejecucion | 2026-05-04 |

---

## Resultados

### Resultados completos (300 queries, 5,183 documentos)

| Metrica | Valor | Meta | Veredicto |
|---------|-------|------|-----------|
| **NDCG@10** | **0.7771** | >= 0.80 | ❌ FAIL |
| MAP@10 | 0.7325 | — | Informativo |
| **Recall@10** | **0.9006** | >= 0.90 | ✅ PASS |
| Precision@10 | 0.1020 | — | Informativo |

### Verificacion con muestras reducidas

| Muestra | NDCG@10 | Recall@10 | Nota |
|---------|---------|-----------|------|
| 500 docs / 100 queries | 0.1169 | 0.1300 | Datos insuficientes — solo 12.1% de juicios de relevancia cubiertos |
| 5,183 docs / 100 queries | 0.7890 | 0.8967 | Mas representativo; NDCG cercano al umbral |
| **5,183 docs / 300 queries** | **0.7771** | **0.9006** | **Resultado definitivo (full)** |

> **Leccion**: El muestreo ingenuo de documentos (primeros N) es invalido para benchmarks IR. Se requiere el corpus completo o un muestreo estratificado que garantice cobertura de los juicios de relevancia (qrels).

---

## Verificacion CE-01 y CE-04

### CE-01: NDCG@10 >= 0.80

```text
Resultado:  0.7771
Umbral:     >= 0.80
Brecha:     -0.0229 (-2.86%)
Veredicto:  ❌ NO CUMPLE
```

### CE-04: Recall@10 >= 0.90

```text
Resultado:  0.9006
Umbral:     >= 0.90
Brecha:     +0.0006 (+0.07%)
Veredicto:  ✅ CUMPLE (margen minimo)
```

---

## Analisis de brecha (gap analysis)

NDCG@10 = 0.7771 esta **2.86% por debajo** del umbral de 0.80. Este resultado es consistente con el estado del arte en dense retrieval para SciFact:

| Sistema | NDCG@10 (SciFact) | Tipo |
|---------|-------------------|------|
| BM25 (sparse baseline) | ~0.665 | Lexical |
| Dense retrieval (BERT-based) | ~0.650–0.720 | Dense |
| Dense + reranking | ~0.730–0.780 | Two-stage |
| **Este benchmark (OpenAI emb + Cosine)** | **0.7771** | Dense-only |
| Meta CE-01 | **>= 0.80** | — |

### Causas probables de la brecha

1. **Naturaleza del dataset**: SciFact requiere verificar si una claim cientifica esta soportada (SUPPORTS) o refutada (REFUTES) por la evidencia. La similitud semantica pura no captura esta relacion de entailment.

2. **Falta de reranking**: Un cross-encoder fine-tuned para scientific claim verification (ej. `allenai/scifact`) podria anadir +0.03–0.08 de NDCG.

3. **Falta de busqueda hibrida**: Combinar BM25 + dense retrieval tipicamente mejora NDCG@10 en +0.02–0.05 en benchmarks BEIR.

4. **Embeddings genericos**: `text-embedding-3-large` es un modelo de proposito general, no fine-tuned para dominio cientifico ni para la tarea especifica de fact-checking.

---

## Limitaciones del benchmark

1. **No es el pipeline completo de Abax-Memory v2.0.0**: Este benchmark evalua exclusivamente el subsistema de busqueda densa. El sistema completo puede incluir componentes adicionales (hybrid search, reranker, filtros) que mejorarian los resultados.

2. **Modelo de embedding fijo**: No se evaluo el impacto de usar modelos alternativos (ej. `voyage-3-large`, `Cohere embed-v3`, modelos open-source fine-tuned para dominio cientifico).

3. **Sin ajuste de hiperparametros**: No se exploraron variaciones en el limite K, threshold de score, o configuracion HNSW.

4. **API key externa**: El benchmark depende de OpenAI API (latencia de red, rate limiting, costos). No mide la latencia end-to-end del sistema.

---

## Recomendaciones

### Para alcanzar CE-01 (NDCG@10 >= 0.80)

| Prioridad | Accion | Impacto estimado | Esfuerzo |
|-----------|--------|-----------------|----------|
| **Alta** | Anadir cross-encoder reranking (top-20 → top-10) | +0.03–0.08 NDCG | Medio |
| **Alta** | Implementar busqueda hibrida (BM25 + dense) | +0.02–0.05 NDCG | Medio |
| **Media** | Evaluar modelos de embedding alternativos fine-tuned para SciFact | +0.02–0.07 NDCG | Bajo (solo evaluacion) |
| **Media** | Ajustar estrategia de chunking (tamano de fragmento, solapamiento) | +0.01–0.03 NDCG | Bajo |
| **Baja** | Fine-tuning de embeddings con el training split de SciFact | +0.05–0.10 NDCG | Alto |

### Para consolidar CE-04

Recall@10 = 0.9006 pasa por 0.07%. Considerar:
- Aumentar K de busqueda en la primera etapa a 20–30 antes del reranking.
- Estrategia de expansion de query (query expansion) para mejorar recall.

---

## Evidencia reproducible

### Comando de ejecucion

```bash
# Crear venv e instalar dependencias
python3 -m venv /tmp/beir_venv
/tmp/beir_venv/bin/pip install beir qdrant-client openai numpy scikit-learn

# Ejecutar benchmark (requiere OPENAI_API_KEY en entorno)
export OPENAI_API_KEY="<redacted>"
/tmp/beir_venv/bin/python3 /tmp/beir_scifact_eval.py
```

### Salida verificable (resumen)

```text
============================================================
BEIR SciFact — FULL Benchmark (300 queries, 5183 docs)
============================================================
NDCG@10:       0.7771  (meta: >= 0.80)
MAP@10:        0.7325
Recall@10:     0.9006  (meta: >= 0.90)
Precision@10:  0.1020
============================================================
❌ CE-01: FAIL (0.7771 < 0.80)
```

### Ambiente verificado

| Componente | Version |
|------------|---------|
| Qdrant | v1.17.1 (Docker, running) |
| Python | 3.12 |
| beir | 2.2.0 |
| qdrant-client | 1.17.1 |
| openai | 2.34.0 |
| numpy | 1.26.x |
| scikit-learn | 1.8.0 |
| SO | Linux |

### Script de evaluacion

El script completo se encuentra en `/tmp/beir_scifact_eval.py`. Contiene la logica de indexacion, busqueda y evaluacion con el modulo `EvaluateRetrieval` de BEIR. **No contiene API keys hardcodeadas** — la key se carga de la variable de entorno `OPENAI_API_KEY`.

---

## Glosario

- **NDCG@10**: Normalized Discounted Cumulative Gain — metrica de ranking que mide la calidad de los top-10 resultados considerando la posicion y relevancia de cada documento.
- **BEIR**: Benchmarking IR — framework estandarizado para evaluar sistemas de recuperacion de informacion (Information Retrieval) en multiples dominios.
- **Qrels**: Relevance judgments — conjunto de pares (query, documento) con etiquetas de relevancia usados como ground truth para calcular metricas.
- **HNSW**: Hierarchical Navigable Small World — algoritmo de indice de grafos para busqueda aproximada de vecinos mas cercanos (ANN) usado por Qdrant.
- **Cross-encoder**: Modelo que procesa pares (query, documento) simultaneamente para puntuar relevancia, mas preciso pero mas costoso que el dense retrieval (bi-encoder).
- **Recall@10**: Fraccion de documentos relevantes recuperados en los primeros 10 resultados. Mide exhaustividad (no deja documentos relevantes fuera del top-10).
