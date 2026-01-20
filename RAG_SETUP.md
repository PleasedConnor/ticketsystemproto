# RAG System Setup Instructions

## Overview
The RAG (Retrieval Augmented Generation) system has been implemented with a component-based architecture. It uses Ollama for embeddings and provides semantic search capabilities.

## Components Implemented

1. **Document Chunking** - Splits articles into searchable chunks
2. **Embedding Service** - Uses Ollama to generate embeddings
3. **Vector Store** - In-memory storage with cosine similarity search
4. **Retrieval Service** - Semantic similarity search
5. **RAG Pipeline** - Orchestrates the entire RAG flow

## Setup Requirements

### 1. Install Ollama Embedding Model

The system uses `nomic-embed-text` model. Make sure it's installed:

```bash
ollama pull nomic-embed-text
```

### 2. Verify Ollama is Running

The embedding service connects to `http://localhost:11434`. Ensure Ollama is running.

### 3. Initial Indexing

When the backend starts, `RAGInitializer` will automatically index all active articles. You should see logs like:

```
Initializing RAG system...
Indexing article '...' into vector store (X chunks)
Successfully indexed X chunks for article '...'
RAG system initialized successfully
```

### 4. How It Works

**When you create/update an article:**
- Article is saved to database
- Article is automatically chunked
- Embeddings are generated for each chunk
- Chunks are stored in the vector store

**When you ask a question in the chatbot:**
- Query is converted to an embedding
- Vector store searches for similar chunks (cosine similarity)
- Top chunks are retrieved and formatted as context
- Context is added to AI prompt along with AI Customisation rules
- AI generates response using both context and customisation rules

## Integration with AI Customisation

The RAG system works seamlessly with your AI Customisation page:

1. **AI Customisation rules** are applied first (General Rules, Escalation Processes, Edge Case Rules)
2. **RAG context** is added next (relevant knowledge base articles)
3. **Base system prompt** follows
4. **Conversation history** is included
5. **Customer message** is processed
6. **AI generates response** using all this context

The AI Customisation rules guide HOW the AI responds, while RAG provides WHAT information to use.

## Fallback Behavior

If the RAG system is unavailable (e.g., Ollama embedding model not installed), the system automatically falls back to keyword-based search. You'll see logs like:

```
RAG: Semantic search failed, falling back to keyword search
```

## Testing

1. Create an article: "Connor Paterson has a Google Pixel 8 with spigen case"
2. Wait for indexing (check logs)
3. Ask chatbot: "What phone does Connor have?"
4. The system should find the article semantically (even though "phone" isn't in the article)

## Troubleshooting

**No embeddings generated:**
- Check if `nomic-embed-text` model is installed: `ollama list`
- Check Ollama is running: `curl http://localhost:11434/api/tags`

**Articles not being indexed:**
- Check backend logs for errors
- Ensure articles are marked as "Active"
- Manually trigger reindexing via API (if needed)

**Semantic search not working:**
- Check if embeddings are being generated (look for "Generated query embedding" in logs)
- Verify vector store has chunks (check "Total chunks in vector store" in logs)
- System will fall back to keyword search if semantic search fails

