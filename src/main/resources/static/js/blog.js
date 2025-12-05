// Blog API utility functions
const BlogAPI = {
    baseUrl: '/api',
    
    // Get auth token from localStorage
    getToken() {
        return localStorage.getItem('token');
    },
    
    // Get headers with auth token
    getHeaders(includeAuth = false) {
        const headers = {
            'Content-Type': 'application/json'
        };
        if (includeAuth) {
            const token = this.getToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        }
        return headers;
    },
    
    // Public Blog APIs
    public: {
        // Get all published blogs with pagination
        getBlogs(page = 0, size = 10) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs?page=${page}&size=${size}`)
                .then(response => response.json());
        },
        
        // Get blog by slug
        getBlogBySlug(slug) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs/slug/${slug}`)
                .then(response => response.json());
        },
        
        // Get blogs by category
        getBlogsByCategory(categoryId, page = 0, size = 10) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs/category/${categoryId}?page=${page}&size=${size}`)
                .then(response => response.json());
        },
        
        // Search blogs
        searchBlogs(keyword, page = 0, size = 10) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
                .then(response => response.json());
        },
        
        // Get featured blogs
        getFeaturedBlogs(size = 5) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs/featured?size=${size}`)
                .then(response => response.json());
        },
        
        // Get top viewed blogs
        getTopViewedBlogs(size = 5) {
            return fetch(`${BlogAPI.baseUrl}/public/blogs/top-viewed?size=${size}`)
                .then(response => response.json());
        },
        
        // Get all active categories
        getCategories() {
            return fetch(`${BlogAPI.baseUrl}/public/blog-categories`)
                .then(response => response.json());
        }
    },
    
    // Manager Blog APIs (require authentication)
    manager: {
        // Get all blogs for manager
        getBlogs(page = 0, size = 10) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs?page=${page}&size=${size}`, {
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Get blog by ID
        getBlogById(id) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}`, {
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Create new blog
        createBlog(blogData) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs`, {
                method: 'POST',
                headers: BlogAPI.getHeaders(true),
                body: JSON.stringify(blogData)
            }).then(response => response.json());
        },
        
        // Update blog
        updateBlog(id, blogData) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}`, {
                method: 'PUT',
                headers: BlogAPI.getHeaders(true),
                body: JSON.stringify(blogData)
            }).then(response => response.json());
        },
        
        // Delete blog (soft delete)
        deleteBlog(id) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}`, {
                method: 'DELETE',
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Publish blog
        publishBlog(id) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}/publish`, {
                method: 'PUT',
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Unpublish blog
        unpublishBlog(id) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}/unpublish`, {
                method: 'PUT',
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Archive blog
        archiveBlog(id) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/${id}/archive`, {
                method: 'PUT',
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Advanced search
        searchBlogs(filters, page = 0, size = 10) {
            const params = new URLSearchParams({
                page: page,
                size: size,
                ...filters
            });
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/search?${params}`, {
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Get my blogs
        getMyBlogs(page = 0, size = 10) {
            return fetch(`${BlogAPI.baseUrl}/manager/blogs/my-blogs?page=${page}&size=${size}`, {
                headers: BlogAPI.getHeaders(true)
            }).then(response => response.json());
        },
        
        // Category APIs
        categories: {
            // Get all categories
            getAll() {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories`, {
                    headers: BlogAPI.getHeaders(true)
                }).then(response => response.json());
            },
            
            // Get category by ID
            getById(id) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/${id}`, {
                    headers: BlogAPI.getHeaders(true)
                }).then(response => response.json());
            },
            
            // Create category
            create(categoryData) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories`, {
                    method: 'POST',
                    headers: BlogAPI.getHeaders(true),
                    body: JSON.stringify(categoryData)
                }).then(response => response.json());
            },
            
            // Update category
            update(id, categoryData) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/${id}`, {
                    method: 'PUT',
                    headers: BlogAPI.getHeaders(true),
                    body: JSON.stringify(categoryData)
                }).then(response => response.json());
            },
            
            // Delete category
            delete(id) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/${id}`, {
                    method: 'DELETE',
                    headers: BlogAPI.getHeaders(true)
                }).then(response => response.json());
            },
            
            // Activate category
            activate(id) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/${id}/activate`, {
                    method: 'PUT',
                    headers: BlogAPI.getHeaders(true)
                }).then(response => response.json());
            },
            
            // Deactivate category
            deactivate(id) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/${id}/deactivate`, {
                    method: 'PUT',
                    headers: BlogAPI.getHeaders(true)
                }).then(response => response.json());
            },
            
            // Reorder categories
            reorder(categoryId1, categoryId2) {
                return fetch(`${BlogAPI.baseUrl}/manager/blog-categories/reorder`, {
                    method: 'POST',
                    headers: BlogAPI.getHeaders(true),
                    body: JSON.stringify({ categoryId1, categoryId2 })
                }).then(response => response.json());
            }
        }
    }
};

// Utility functions
const BlogUtils = {
    // Format date to Vietnamese locale
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    },
    
    // Format date with time
    formatDateTime(dateString) {
        const date = new Date(dateString);
        return date.toLocaleString('vi-VN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    },
    
    // Format time ago (e.g., "2 giờ trước")
    formatTimeAgo(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now - date) / 1000);
        
        if (seconds < 60) return 'Vừa xong';
        if (seconds < 3600) return Math.floor(seconds / 60) + ' phút trước';
        if (seconds < 86400) return Math.floor(seconds / 3600) + ' giờ trước';
        if (seconds < 604800) return Math.floor(seconds / 86400) + ' ngày trước';
        if (seconds < 2592000) return Math.floor(seconds / 604800) + ' tuần trước';
        if (seconds < 31536000) return Math.floor(seconds / 2592000) + ' tháng trước';
        return Math.floor(seconds / 31536000) + ' năm trước';
    },
    
    // Truncate text
    truncate(text, maxLength) {
        if (!text) return '';
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    },
    
    // Strip HTML tags
    stripHtml(html) {
        const tmp = document.createElement('div');
        tmp.innerHTML = html;
        return tmp.textContent || tmp.innerText || '';
    },
    
    // Calculate reading time
    calculateReadingTime(content) {
        const wordsPerMinute = 200;
        const text = this.stripHtml(content);
        const wordCount = text.split(/\s+/).length;
        return Math.ceil(wordCount / wordsPerMinute);
    },
    
    // Generate slug from title
    generateSlug(title) {
        return title
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '') // Remove accents
            .replace(/đ/g, 'd')
            .replace(/[^a-z0-9\s-]/g, '')
            .trim()
            .replace(/\s+/g, '-')
            .replace(/-+/g, '-');
    },
    
    // Show loading spinner
    showLoading(containerId) {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <div class="spinner-border" role="status">
                        <span class="visually-hidden">Đang tải...</span>
                    </div>
                </div>
            `;
        }
    },
    
    // Show error message
    showError(containerId, message = 'Có lỗi xảy ra') {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `
                <div class="alert alert-danger" role="alert">
                    <i class="bi bi-exclamation-triangle"></i> ${message}
                </div>
            `;
        }
    },
    
    // Show empty state
    showEmpty(containerId, message = 'Không có dữ liệu') {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `
                <div class="alert alert-info" role="alert">
                    <i class="bi bi-info-circle"></i> ${message}
                </div>
            `;
        }
    },
    
    // Show success toast
    showToast(message, type = 'success') {
        // Check if Bootstrap toast container exists
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }
        
        const toastId = 'toast-' + Date.now();
        const bgColor = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-info';
        
        const toastHtml = `
            <div id="${toastId}" class="toast ${bgColor} text-white" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header ${bgColor} text-white">
                    <strong class="me-auto">${type === 'success' ? 'Thành công' : type === 'error' ? 'Lỗi' : 'Thông báo'}</strong>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { autohide: true, delay: 3000 });
        toast.show();
        
        // Remove toast element after it's hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { BlogAPI, BlogUtils };
}
