# RAG Implementation Plan - Component Architecture

## Recommended Approach: Ollama Embeddings + In-Memory Vector Store

Since you already have Ollama running, we can use it for embeddings, which is simpler than ONNX Runtime.

## Architecture Components

### 1. Input Processing
- **DocumentChunker**: Split articles into chunks (500 chars, 100 char overlap)
- **TextNormalizer**: Clean HTML, normalize text

### 2. Embedding & Storage  
- **EmbeddingService**: Call Ollama's embedding API
- **VectorStore**: In-memory storage with cosine similarity search
- **EmbeddingCache**: Cache embeddings in database/file

### 3. Retrieval
- **RetrievalService**: Semantic similarity search
- **Reranker** (optional): Improve relevance

### 4. Orchestration
- **RAGPipeline**: Coordinate the entire flow
- **ContextBuilder**: Format retrieved chunks

### 5. AI Generation
- **AIService**: Already exists, integrate RAG context

## Implementation Steps

### Step 1: Create Component Interfaces
- `DocumentChunker` interface
- `EmbeddingService` interface  
- `VectorStore` interface
- `RetrievalService` interface
- `RAGPipeline` interface

### Step 2: Implement Embedding Service
- Use Ollama's `/api/embeddings` endpoint
- Model: `nomic-embed-text` (good quality, fast)
- Cache embeddings to avoid re-computation

### Step 3: Implement Vector Store
- In-memory storage (Map-based)
- Cosine similarity calculation
- Top-K retrieval

### Step 4: Implement Document Processing
- Chunk articles on save/update
- Store chunks with metadata (article ID, chunk index)

### Step 5: Implement RAG Pipeline
- Query → Embed → Search → Retrieve → Context → Generate

### Step 6: Integrate with AIService
- Replace keyword search with semantic search
- Keep backward compatibility

## File Structure

```
backend/src/main/java/com/prototype/
├── rag/
│   ├── chunking/
│   │   ├── DocumentChunker.java
│   │   └── TextChunker.java
│   ├── embedding/
│   │   ├── EmbeddingService.java
│   │   └── OllamaEmbeddingService.java
│   ├── storage/
│   │   ├── VectorStore.java
│   │   ├── InMemoryVectorStore.java
│   │   └── DocumentChunk.java
│   ├── retrieval/
│   │   ├── RetrievalService.java
│   │   └── SemanticRetrievalService.java
│   └── pipeline/
│       ├── RAGPipeline.java
│       └── RAGPipelineImpl.java
```

## Dependencies

No new dependencies needed! We'll use:
- Existing WebClient (for Ollama API)
- Java standard library (for vector math)
- Existing Spring Boot (for dependency injection)

## Benefits

1. **Uses existing Ollama** - No new services needed
2. **True semantic search** - Understands meaning
3. **Component-based** - Easy to extend/swap
4. **Local & free** - No external APIs
5. **Fast** - In-memory search is very fast

## Next Steps

Would you like me to:
1. Start implementing the component interfaces?
2. Create the embedding service using Ollama?
3. Build the vector store and retrieval system?
4. Integrate everything into the RAG pipeline?

