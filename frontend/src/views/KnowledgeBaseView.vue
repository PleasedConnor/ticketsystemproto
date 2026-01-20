<template>
  <div class="knowledge-base-container">
    <div class="kb-content">
      <!-- Header -->
      <header class="kb-header">
        <div class="header-content">
          <h1>Knowledge Base</h1>
          <div class="header-actions">
            <button class="btn btn-secondary" @click="showCreateCategoryDialog = true">
              + Add Category
            </button>
            <button class="btn btn-primary" @click="showCreateArticleDialog = true">
              + Create Article
            </button>
          </div>
        </div>
      </header>

      <div class="kb-layout">
        <!-- Categories Sidebar -->
        <aside class="categories-sidebar">
          <div class="categories-header">
            <h2>Categories</h2>
          </div>
          <div class="categories-list">
            <div 
              class="category-item" 
              :class="{ active: selectedCategoryId === null }"
              @click="selectCategory(null)"
            >
              <span>All Articles</span>
              <span class="article-count">({{ articles.length }})</span>
            </div>
            <div
              v-for="category in categories"
              :key="category.id"
              class="category-group"
            >
              <div
                class="category-item category-header"
                :class="{ active: selectedCategoryId === category.id, expanded: expandedCategories.has(category.id) }"
                @click.stop="toggleCategory(category.id)"
              >
                <div class="category-name-row">
                  <span class="expand-icon">▶</span>
                  <span>{{ category.name }}</span>
                </div>
                <div class="category-actions">
                  <span class="article-count">({{ getArticleCountForCategory(category.id) }})</span>
                  <button 
                    class="btn-icon-small" 
                    @click.stop="createArticleInCategory(category.id)"
                    title="Add Article to Category"
                  >
                    +
                  </button>
                </div>
              </div>
              <div v-if="expandedCategories.has(category.id)" class="category-articles">
                <div
                  v-for="article in getArticlesForCategory(category.id)"
                  :key="article.id"
                  class="category-article-item"
                  :class="{ active: selectedArticle?.id === article.id }"
                  @click="selectArticle(article)"
                >
                  <span>{{ article.title }}</span>
                </div>
                <div v-if="getArticlesForCategory(category.id).length === 0" class="empty-category">
                  No articles in this category
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- Articles List -->
        <div class="articles-section">
          <div v-if="filteredArticles.length === 0" class="empty-state">
            <p>No articles found. Create your first article to get started.</p>
          </div>
          <div v-else class="articles-list">
            <div
              v-for="article in filteredArticles"
              :key="article.id"
              class="article-card"
              :class="{ active: selectedArticle?.id === article.id }"
              @click="selectArticle(article)"
            >
              <div class="article-card-header">
                <h3>{{ article.title }}</h3>
                <span v-if="article.category" class="category-badge">
                  {{ article.category.name }}
                </span>
              </div>
              <div class="article-card-preview" v-html="getPreview(article.content)"></div>
              <div class="article-card-footer">
                <span class="article-date">{{ formatDate(article.updatedAt) }}</span>
                <span :class="['article-status', article.isActive ? 'active' : 'inactive']">
                  {{ article.isActive ? 'Active' : 'Inactive' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Article Editor -->
        <div v-if="selectedArticle" class="article-editor">
          <div class="editor-header">
            <h2>{{ editingArticle ? 'Edit Article' : 'View Article' }}</h2>
            <div class="editor-actions">
              <button v-if="!editingArticle" class="btn btn-secondary" @click="startEditing">
                Edit
              </button>
              <button v-if="editingArticle" class="btn btn-primary" @click="saveArticle" :disabled="saving">
                {{ saving ? 'Saving...' : 'Save' }}
              </button>
              <button v-if="editingArticle" class="btn btn-secondary" @click="cancelEditing">
                Cancel
              </button>
              <button class="btn btn-danger" @click="handleDeleteArticle(selectedArticle.id)">
                Delete
              </button>
            </div>
          </div>

          <div v-if="editingArticle" class="editor-form">
            <div class="form-group">
              <label class="form-label">Title</label>
              <input v-model="articleForm.title" class="form-control" type="text" />
            </div>
            <div class="form-group">
              <label class="form-label">Category</label>
              <select v-model="articleForm.categoryId" class="form-control">
                <option :value="null">No Category</option>
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Content</label>
              <div class="editor-toolbar">
                <button @click="formatText('bold')" class="toolbar-btn" title="Bold">
                  <strong>B</strong>
                </button>
                <button @click="formatText('italic')" class="toolbar-btn" title="Italic">
                  <em>I</em>
                </button>
                <button @click="formatText('underline')" class="toolbar-btn" title="Underline">
                  <u>U</u>
                </button>
                <button @click="formatText('formatBlock', 'h2')" class="toolbar-btn" title="Heading">
                  H2
                </button>
                <button @click="formatText('formatBlock', 'h3')" class="toolbar-btn" title="Subheading">
                  H3
                </button>
                <button @click="formatText('insertUnorderedList')" class="toolbar-btn" title="Bullet List">
                  •
                </button>
                <button @click="formatText('insertOrderedList')" class="toolbar-btn" title="Numbered List">
                  1.
                </button>
              </div>
              <div
                ref="editorRef"
                contenteditable="true"
                class="rich-text-editor"
                @input="updateContent"
                v-html="articleForm.content"
              ></div>
            </div>
            <div class="form-group">
              <label class="form-label">
                <input type="checkbox" v-model="articleForm.isActive" />
                Active
              </label>
            </div>
          </div>
          <div v-else class="article-view">
            <h2>{{ selectedArticle.title }}</h2>
            <div v-if="selectedArticle.category" class="article-category">
              Category: {{ selectedArticle.category.name }}
            </div>
            <div class="article-content" v-html="selectedArticle.content"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Category Dialog -->
    <div v-if="showCreateCategoryDialog" class="dialog-overlay" @click="showCreateCategoryDialog = false">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <h3>Create Category</h3>
          <button class="close-btn" @click="showCreateCategoryDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label class="form-label">Category Name</label>
            <input v-model="categoryForm.name" class="form-control" type="text" />
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea v-model="categoryForm.description" class="form-control" rows="3"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-secondary" @click="showCreateCategoryDialog = false">Cancel</button>
          <button class="btn btn-primary" @click="handleCreateCategory">Create</button>
        </div>
      </div>
    </div>

    <!-- Create Article Dialog -->
    <div v-if="showCreateArticleDialog" class="dialog-overlay" @click="showCreateArticleDialog = false">
      <div class="dialog-content large" @click.stop>
        <div class="dialog-header">
          <h3>Create Article</h3>
          <button class="close-btn" @click="showCreateArticleDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label class="form-label">Title</label>
            <input v-model="newArticleForm.title" class="form-control" type="text" />
          </div>
          <div class="form-group">
            <label class="form-label">Category</label>
            <select v-model="newArticleForm.categoryId" class="form-control">
              <option :value="null">No Category</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Content</label>
            <div class="editor-toolbar">
              <button @click="formatText('bold', null, 'new')" class="toolbar-btn" title="Bold">
                <strong>B</strong>
              </button>
              <button @click="formatText('italic', null, 'new')" class="toolbar-btn" title="Italic">
                <em>I</em>
              </button>
              <button @click="formatText('underline', null, 'new')" class="toolbar-btn" title="Underline">
                <u>U</u>
              </button>
              <button @click="formatText('formatBlock', 'h2', 'new')" class="toolbar-btn" title="Heading">
                H2
              </button>
              <button @click="formatText('formatBlock', 'h3', 'new')" class="toolbar-btn" title="Subheading">
                H3
              </button>
              <button @click="formatText('insertUnorderedList', null, 'new')" class="toolbar-btn" title="Bullet List">
                •
              </button>
              <button @click="formatText('insertOrderedList', null, 'new')" class="toolbar-btn" title="Numbered List">
                1.
              </button>
            </div>
            <div
              ref="newEditorRef"
              contenteditable="true"
              class="rich-text-editor"
              @input="updateNewContent"
            ></div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn btn-secondary" @click="closeCreateArticleDialog">Cancel</button>
          <button class="btn btn-primary" @click="handleCreateArticle">Create</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useBackendApi } from '@/composables/useBackendApi'

const {
  getAllCategories,
  createCategory,
  deleteCategory,
  getAllArticles,
  createArticle,
  updateArticle,
  deleteArticle
} = useBackendApi()

const categories = ref<any[]>([])
const articles = ref<any[]>([])
const selectedCategoryId = ref<number | null>(null)
const selectedArticle = ref<any>(null)
const editingArticle = ref(false)
const saving = ref(false)
const showCreateCategoryDialog = ref(false)
const showCreateArticleDialog = ref(false)
const expandedCategories = ref<Set<number>>(new Set())

const editorRef = ref<HTMLElement | null>(null)
const newEditorRef = ref<HTMLElement | null>(null)

const categoryForm = ref({
  name: '',
  description: ''
})

const articleForm = ref({
  title: '',
  content: '',
  categoryId: null as number | null,
  isActive: true
})

const newArticleForm = ref({
  title: '',
  content: '',
  categoryId: null as number | null
})

const filteredArticles = computed(() => {
  if (selectedCategoryId.value === null) {
    return articles.value
  }
  return articles.value.filter(a => a.category?.id === selectedCategoryId.value)
})

const loadCategories = async () => {
  try {
    console.log('Loading categories...')
    const response = await getAllCategories()
    console.log('Categories loaded:', response.data)
    categories.value = response.data || []
    console.log('Categories array updated:', categories.value)
  } catch (error) {
    console.error('Failed to load categories:', error)
    console.error('Error details:', error)
    categories.value = []
  }
}

const loadArticles = async () => {
  try {
    console.log('Loading articles...')
    const response = await getAllArticles()
    console.log('Articles loaded:', response.data)
    articles.value = response.data || []
    console.log('Articles array updated, total:', articles.value.length)
    // Log category associations
    articles.value.forEach(article => {
      console.log(`Article "${article.title}" - Category ID: ${article.category?.id || article.categoryId || 'none'}`)
    })
  } catch (error) {
    console.error('Failed to load articles:', error)
    articles.value = []
  }
}

const toggleCategory = (categoryId: number) => {
  if (expandedCategories.value.has(categoryId)) {
    expandedCategories.value.delete(categoryId)
  } else {
    expandedCategories.value.add(categoryId)
  }
  selectedCategoryId.value = categoryId
  selectedArticle.value = null
  editingArticle.value = false
}

const selectCategory = (categoryId: number | null) => {
  selectedCategoryId.value = categoryId
  selectedArticle.value = null
  editingArticle.value = false
}

const getArticlesForCategory = (categoryId: number) => {
  const filtered = articles.value.filter(a => {
    const articleCategoryId = a.category?.id || a.categoryId
    return articleCategoryId === categoryId
  })
  console.log(`Articles for category ${categoryId}:`, filtered)
  return filtered
}

const createArticleInCategory = (categoryId: number) => {
  newArticleForm.value.categoryId = categoryId
  showCreateArticleDialog.value = true
}

const closeCreateArticleDialog = () => {
  newArticleForm.value = { title: '', content: '', categoryId: null }
  if (newEditorRef.value) {
    newEditorRef.value.innerHTML = ''
  }
  showCreateArticleDialog.value = false
}

const selectArticle = (article: any) => {
  selectedArticle.value = article
  editingArticle.value = false
}

const getArticleCountForCategory = (categoryId: number) => {
  const count = articles.value.filter(a => {
    const articleCategoryId = a.category?.id || a.categoryId
    return articleCategoryId === categoryId
  }).length
  return count
}

const getPreview = (content: string) => {
  if (!content) return ''
  // Strip HTML tags and get first 150 characters
  const text = content.replace(/<[^>]*>/g, '')
  return text.length > 150 ? text.substring(0, 150) + '...' : text
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const handleCreateCategory = async () => {
  if (!categoryForm.value.name.trim()) {
    alert('Please enter a category name')
    return
  }
  try {
    console.log('Creating category with data:', categoryForm.value)
    const response = await createCategory(categoryForm.value)
    console.log('Category created successfully:', response.data)
    categoryForm.value = { name: '', description: '' }
    showCreateCategoryDialog.value = false
    // Force reload categories after a short delay to ensure database is updated
    setTimeout(async () => {
      await loadCategories()
    }, 100)
  } catch (error: any) {
    console.error('Failed to create category:', error)
    console.error('Error response:', error.response?.data)
    console.error('Error status:', error.response?.status)
    console.error('Full error:', error)
    
    // Extract error message from response
    let errorMessage = 'Unknown error'
    if (error.response?.data) {
      if (error.response.data.error) {
        errorMessage = error.response.data.error
      } else if (error.response.data.message) {
        errorMessage = error.response.data.message
      } else if (typeof error.response.data === 'string') {
        errorMessage = error.response.data
      }
    } else if (error.message) {
      errorMessage = error.message
    }
    
    alert(`Failed to create category: ${errorMessage}`)
  }
}

const startEditing = () => {
  if (!selectedArticle.value) return
  editingArticle.value = true
  articleForm.value = {
    title: selectedArticle.value.title,
    content: selectedArticle.value.content,
    categoryId: selectedArticle.value.category?.id || null,
    isActive: selectedArticle.value.isActive
  }
  nextTick(() => {
    if (editorRef.value) {
      editorRef.value.innerHTML = articleForm.value.content
    }
  })
}

const cancelEditing = () => {
  editingArticle.value = false
  articleForm.value = {
    title: '',
    content: '',
    categoryId: null,
    isActive: true
  }
}

const updateContent = () => {
  if (editorRef.value) {
    articleForm.value.content = editorRef.value.innerHTML
  }
}

const updateNewContent = () => {
  if (newEditorRef.value) {
    let content = newEditorRef.value.innerHTML
    // Remove empty paragraphs and br tags that might be added by contenteditable
    content = content.replace(/<p><br><\/p>/g, '')
    content = content.replace(/<div><br><\/div>/g, '')
    newArticleForm.value.content = content
  }
}

const formatText = (command: string, value: string | null = null, editor: 'current' | 'new' = 'current') => {
  const editorElement = editor === 'new' ? newEditorRef.value : editorRef.value
  if (!editorElement) return
  
  editorElement.focus()
  document.execCommand(command, false, value || undefined)
  if (editor === 'new') {
    updateNewContent()
  } else {
    updateContent()
  }
}

const saveArticle = async () => {
  if (!selectedArticle.value || !articleForm.value.title.trim()) {
    alert('Please enter a title')
    return
  }
  saving.value = true
  try {
    const articleData = {
      title: articleForm.value.title,
      content: articleForm.value.content,
      category: articleForm.value.categoryId ? { id: articleForm.value.categoryId } : null,
      isActive: articleForm.value.isActive
    }
    await updateArticle(selectedArticle.value.id, articleData)
    await loadArticles()
    const updated = articles.value.find(a => a.id === selectedArticle.value.id)
    if (updated) {
      selectedArticle.value = updated
    }
    editingArticle.value = false
  } catch (error) {
    console.error('Failed to save article:', error)
    alert('Failed to save article. Please try again.')
  } finally {
    saving.value = false
  }
}

const handleCreateArticle = async () => {
  if (!newArticleForm.value.title.trim()) {
    alert('Please enter a title')
    return
  }
  
  // Get content from editor if not already set
  if (newEditorRef.value) {
    newArticleForm.value.content = newEditorRef.value.innerHTML
  }
  
  // Strip HTML tags to check if there's actual content
  const textContent = newArticleForm.value.content.replace(/<[^>]*>/g, '').trim()
  if (!textContent) {
    alert('Please enter content for the article')
    return
  }
  
  try {
    const articleData = {
      title: newArticleForm.value.title.trim(),
      content: newArticleForm.value.content,
      category: newArticleForm.value.categoryId ? { id: newArticleForm.value.categoryId } : null,
      isActive: true
    }
    
    console.log('Creating article with data:', articleData)
    const response = await createArticle(articleData)
    console.log('Article created successfully:', response.data)
    
    const createdCategoryId = newArticleForm.value.categoryId
    newArticleForm.value = { title: '', content: '', categoryId: null }
    if (newEditorRef.value) {
      newEditorRef.value.innerHTML = ''
    }
    showCreateArticleDialog.value = false
    
    // Reload articles after a short delay to ensure database is updated
    setTimeout(async () => {
      await loadArticles()
      // Expand the category if article was created in a specific category
      if (createdCategoryId) {
        expandedCategories.value.add(createdCategoryId)
        selectedCategoryId.value = createdCategoryId
        // Select the newly created article
        const newArticle = articles.value.find(a => {
          const articleCategoryId = a.category?.id || a.categoryId
          return articleCategoryId === createdCategoryId && a.title === articleData.title
        })
        if (newArticle) {
          selectedArticle.value = newArticle
        }
      }
    }, 200)
  } catch (error: any) {
    console.error('Failed to create article:', error)
    console.error('Error response:', error.response?.data)
    console.error('Error status:', error.response?.status)
    const errorMessage = error.response?.data?.message || error.message || 'Unknown error'
    alert(`Failed to create article: ${errorMessage}`)
  }
}

const handleDeleteArticle = async (id: number) => {
  if (!confirm('Are you sure you want to delete this article?')) {
    return
  }
  try {
    await deleteArticle(id)
    if (selectedArticle.value?.id === id) {
      selectedArticle.value = null
    }
    await loadArticles()
  } catch (error) {
    console.error('Failed to delete article:', error)
    alert('Failed to delete article. Please try again.')
  }
}

onMounted(() => {
  loadCategories()
  loadArticles()
})
</script>

<style scoped>
.knowledge-base-container {
  padding: 2rem;
  max-width: 1800px;
  margin: 0 auto;
}

.kb-header {
  margin-bottom: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-header h1 {
  margin: 0;
  font-size: 2rem;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.kb-layout {
  display: grid;
  grid-template-columns: 250px 1fr 1fr;
  gap: 2rem;
  min-height: 600px;
}

.categories-sidebar {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  height: fit-content;
  position: sticky;
  top: 80px;
}

.categories-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.categories-header h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #333;
}

.btn-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #f8f9fa;
  border-color: #dc3545;
  color: #dc3545;
}

.categories-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.category-group {
  display: flex;
  flex-direction: column;
}

.category-item {
  padding: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-item:hover {
  background: #f8f9fa;
}

.category-item.active {
  background: #fff5f5;
  color: #dc3545;
  font-weight: 600;
}

.category-header {
  user-select: none;
}

.category-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.expand-icon {
  font-size: 0.7rem;
  transition: transform 0.2s;
  color: #666;
  display: inline-block;
}

.category-header.expanded .expand-icon {
  transform: rotate(90deg);
}

.category-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-icon-small {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}

.btn-icon-small:hover {
  background: #dc3545;
  border-color: #dc3545;
  color: white;
}

.category-articles {
  margin-left: 1.5rem;
  margin-top: 0.25rem;
  padding-left: 0.75rem;
  border-left: 2px solid #e9ecef;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.category-article-item {
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
  color: #555;
}

.category-article-item:hover {
  background: #f8f9fa;
  color: #dc3545;
}

.category-article-item.active {
  background: #fff5f5;
  color: #dc3545;
  font-weight: 500;
}

.empty-category {
  padding: 0.5rem 0.75rem;
  color: #999;
  font-size: 0.85rem;
  font-style: italic;
}

.article-count {
  font-size: 0.85rem;
  color: #666;
  font-weight: normal;
}

.category-item.active .article-count {
  color: #dc3545;
}

.articles-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  max-height: 800px;
  overflow-y: auto;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.article-card {
  padding: 1rem;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.article-card:hover {
  border-color: #dc3545;
  box-shadow: 0 2px 8px rgba(220, 53, 69, 0.1);
}

.article-card.active {
  border-color: #dc3545;
  background: #fff5f5;
}

.article-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.article-card-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #333;
}

.category-badge {
  background: #dc3545;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
}

.article-card-preview {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.article-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #999;
}

.article-status.active {
  color: #28a745;
}

.article-status.inactive {
  color: #dc3545;
}

.article-editor {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  max-height: 800px;
  overflow-y: auto;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e9ecef;
}

.editor-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.editor-actions {
  display: flex;
  gap: 0.5rem;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.article-view h2 {
  margin: 0 0 0.5rem 0;
  color: #333;
}

.article-category {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.article-content {
  line-height: 1.8;
  color: #333;
}

.article-content :deep(h2) {
  font-size: 1.5rem;
  margin: 1.5rem 0 1rem 0;
  color: #333;
}

.article-content :deep(h3) {
  font-size: 1.2rem;
  margin: 1.2rem 0 0.8rem 0;
  color: #333;
}

.article-content :deep(p) {
  margin: 0.8rem 0;
}

.article-content :deep(ul), .article-content :deep(ol) {
  margin: 0.8rem 0;
  padding-left: 2rem;
}

.article-content :deep(li) {
  margin: 0.4rem 0;
}

.editor-toolbar {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px 4px 0 0;
  border-bottom: none;
}

.toolbar-btn {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.toolbar-btn:hover {
  background: #f8f9fa;
  border-color: #dc3545;
}

.rich-text-editor {
  min-height: 300px;
  padding: 1rem;
  border: 1px solid #e9ecef;
  border-radius: 0 0 4px 4px;
  background: white;
  outline: none;
  line-height: 1.6;
}

.rich-text-editor:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.dialog-content.large {
  max-width: 800px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.dialog-header h3 {
  margin: 0;
  font-size: 1.3rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f8f9fa;
  color: #dc3545;
}

.dialog-body {
  padding: 1.5rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

@media (max-width: 1400px) {
  .kb-layout {
    grid-template-columns: 200px 1fr;
  }
  
  .article-editor {
    grid-column: 1 / -1;
    margin-top: 2rem;
  }
}

@media (max-width: 768px) {
  .kb-layout {
    grid-template-columns: 1fr;
  }
  
  .categories-sidebar {
    position: static;
  }
}
</style>

