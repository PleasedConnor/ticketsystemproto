# RAG Architecture Proposal - Component-Based System

## Overview
A locally-hosted, cost-free RAG system using a component architecture similar to LangChain, built for Java/Spring Boot.

## Architecture Components

### 1. Input Processing Layer
**Purpose**: Transform raw data (articles) into structured, searchable documents

**Components**:
- **Document Loader**: Load articles from database
- **Text Splitter**: Chunk articles into smaller pieces (for better retrieval)
- **Text Normalizer**: Clean and normalize text (remove HTML, normalize whitespace)

**Implementation**:
- Use existing `Article` entities from database
- Implement chunking strategy (e.g., 500-char chunks with 100-char overlap)
- Strip HTML and normalize text

### 2. Embedding & Storage Layer
**Purpose**: Convert text into vector representations and store them

**Components**:
- **Embedding Model**: Convert text → vectors
- **Vector Store**: Store and index vectors for fast similarity search

**Options for Java**:

#### Option A: ONNX Runtime + Sentence Transformers (Recommended)
- **Embedding Model**: Use ONNX Runtime Java to run pre-trained models
- **Models**: `all-MiniLM-L6-v2` (80MB, fast, good quality) or `sentence-transformers/all-mpnet-base-v2`
- **Vector Store**: In-memory with similarity search, or Chroma/Qdrant

#### Option B: Chroma (Python service, Java client)
- **Embedding**: Chroma handles embeddings internally
- **Vector Store**: Chroma (lightweight, local)
- **Integration**: Call Chroma via HTTP API

#### Option C: Qdrant (Rust-based, Java client)
- **Embedding**: Use ONNX Runtime or call embedding service
- **Vector Store**: Qdrant (lightweight, local, fast)
- **Integration**: Qdrant Java client

**Recommended**: Option A (ONNX Runtime) + In-memory vector store for simplicity

### 3. Retrieval Layer
**Purpose**: Find relevant documents based on semantic similarity

**Components**:
- **Query Embedder**: Convert user query to vector
- **Similarity Search**: Find most similar document chunks
- **Reranking** (optional): Improve relevance with cross-encoder

**Implementation**:
- Embed user query using same model
- Calculate cosine similarity between query vector and document vectors
- Return top-k most similar chunks

### 4. Orchestration Layer
**Purpose**: Coordinate the RAG pipeline

**Components**:
- **RAG Pipeline**: Orchestrate retrieval → context building → generation
- **Context Builder**: Format retrieved chunks for AI prompt
- **Memory Management**: Track what's been retrieved, avoid duplicates

### 5. AI Generation Layer
**Purpose**: Generate responses using retrieved context

**Components**:
- **Prompt Builder**: Construct prompts with retrieved context
- **LLM**: Ollama (already integrated)
- **Response Formatter**: Format and clean AI responses

## Proposed Implementation Stack

### Core Technologies

1. **Embedding Model**: 
   - **ONNX Runtime Java** + **sentence-transformers/all-MiniLM-L6-v2**
   - Local, no API costs
   - Fast inference (~10-50ms per embedding)

2. **Vector Storage**:
   - **In-memory** (for small-medium datasets)
   - Or **Chroma** (lightweight, local, persistent)
   - Or **Qdrant** (production-ready, local)

3. **Text Processing**:
   - **Apache Tika** (for complex documents)
   - **Simple text processing** (for HTML content)

4. **Similarity Search**:
   - **Cosine similarity** (simple, effective)
   - **FAISS-like** indexing (if needed for scale)

### Component Structure

```
┌─────────────────────────────────────────────────────────┐
│                    Input Processing                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Document     │→ │ Text         │→ │ Text         │  │
│  │ Loader       │  │ Splitter     │  │ Normalizer   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              Embedding & Storage                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Embedding    │→ │ Vector       │→ │ Vector       │  │
│  │ Model        │  │ Generator    │  │ Store        │  │
│  │ (ONNX)       │  │              │  │ (In-memory/  │  │
│  └──────────────┘  └──────────────┘  │  Chroma)      │  │
│                                      └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Retrieval                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Query        │→ │ Similarity    │→ │ Top-K        │  │
│  │ Embedder     │  │ Search        │  │ Selector     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Orchestration                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ RAG          │→ │ Context      │→ │ Memory       │  │
│  │ Pipeline     │  │ Builder      │  │ Manager      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  AI Generation                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Prompt       │→ │ LLM          │→ │ Response     │  │
│  │ Builder      │  │ (Ollama)     │  │ Formatter    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Implementation Plan

### Phase 1: Embedding Infrastructure
1. Add ONNX Runtime Java dependency
2. Download embedding model (all-MiniLM-L6-v2)
3. Create `EmbeddingService` to generate embeddings
4. Store embeddings with article chunks

### Phase 2: Vector Storage
1. Create `VectorStore` interface
2. Implement in-memory vector store (for MVP)
3. Optionally add Chroma/Qdrant integration

### Phase 3: Retrieval System
1. Create `RetrievalService` for similarity search
2. Implement chunking strategy
3. Add reranking (optional)

### Phase 4: RAG Pipeline
1. Create `RAGPipeline` orchestrator
2. Integrate with existing `AIService`
3. Replace keyword search with semantic search

### Phase 5: Persistence
1. Store embeddings in database or file
2. Implement embedding regeneration on article updates
3. Add caching for performance

## Dependencies Needed

```xml
<!-- ONNX Runtime Java -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.16.3</version>
</dependency>

<!-- Optional: Chroma Java Client -->
<dependency>
    <groupId>io.github.trychroma</groupId>
    <artifactId>chroma-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Benefits

1. **True Semantic Search**: Understands meaning, not just keywords
2. **Local & Free**: No API costs, runs entirely locally
3. **Component-Based**: Modular, easy to extend
4. **Scalable**: Can swap components (e.g., in-memory → Chroma → Qdrant)
5. **Production-Ready**: Similar architecture to LangChain

## Next Steps

1. Implement `EmbeddingService` with ONNX Runtime
2. Create `VectorStore` interface and in-memory implementation
3. Build `RetrievalService` for semantic search
4. Create `RAGPipeline` to orchestrate everything
5. Integrate with existing `AIService`

Would you like me to start implementing this architecture?

