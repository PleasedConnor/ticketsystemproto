# How to View Backend Logs

## Option 1: View Log File (Recommended)
The backend logs are written to `backend.log` in the project root directory.

### View last 50 lines:
```bash
tail -50 backend.log
```

### View last 100 lines and filter for RAG/search:
```bash
tail -100 backend.log | grep -i "rag\|knowledge\|search\|article"
```

### Follow logs in real-time (like `tail -f`):
```bash
tail -f backend.log
```

### Search for specific terms:
```bash
grep -i "rag" backend.log
grep -i "searching articles" backend.log
```

## Option 2: View Console Output
If you're running the backend with `./scripts/dev.sh`, the logs also appear in the terminal where the script is running.

## Option 3: Check Application Logs
The Spring Boot application logs to console by default. Check the terminal where you started the backend server.

## Common Log Messages to Look For:
- `RAG: Searching knowledge base for: '...'` - Shows what query is being searched
- `Searching articles with query: '...'` - Shows the search query
- `Search keywords: [...]` - Shows extracted keywords
- `Found X relevant articles` - Shows how many articles matched
- `RAG: Found X relevant articles` - Shows RAG results
- `RAG: Generated context` - Shows that context was created
- `getActiveArticles: Found X active articles` - Shows how many active articles exist

