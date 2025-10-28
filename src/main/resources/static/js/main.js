/**
 * Inscrepper - Main JavaScript Functions
 * Enhanced with Bootstrap integration and modern features
 */

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

/**
 * Initialize all app functions
 */
function initializeApp() {
    initializeTooltips();
    initializeHealthCheck();
    initializeFormValidation();
    initializeAnimations();
    initializeUserFeedback();
}

/**
 * Initialize Bootstrap tooltips
 */
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

/**
 * Health check functionality
 */
function initializeHealthCheck() {
    const healthIndicator = document.getElementById('health-status');
    if (!healthIndicator) return;

    checkHealth();
    // Check health every 30 seconds
    setInterval(checkHealth, 30000);
}

async function checkHealth() {
    const indicator = document.getElementById('health-status');
    if (!indicator) return;

    try {
        const response = await fetch('/health');
        const data = await response.json();
        
        if (response.ok) {
            updateHealthStatus('success', 'Online', 'Sistema funcionando normalmente');
        } else {
            updateHealthStatus('warning', 'Instável', 'Sistema com problemas');
        }
    } catch (error) {
        updateHealthStatus('danger', 'Offline', 'Sistema indisponível');
    }
}

function updateHealthStatus(type, status, message) {
    const indicator = document.getElementById('health-status');
    if (!indicator) return;

    indicator.className = `badge bg-${type}`;
    indicator.textContent = status;
    indicator.title = message;
}

/**
 * Enhanced form validation
 */
function initializeFormValidation() {
    const forms = document.querySelectorAll('.needs-validation');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                
                // Focus on first invalid field
                const firstInvalid = form.querySelector(':invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            }
            
            form.classList.add('was-validated');
        });

        // Real-time validation
        const inputs = form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('blur', function() {
                if (form.classList.contains('was-validated')) {
                    input.classList.toggle('is-valid', input.checkValidity());
                    input.classList.toggle('is-invalid', !input.checkValidity());
                }
            });
        });
    });
}

/**
 * Initialize smooth animations
 */
function initializeAnimations() {
    // Animate cards on scroll
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, observerOptions);

    // Observe cards for animation
    const cards = document.querySelectorAll('.card');
    cards.forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        card.style.transition = 'all 0.6s ease-out';
        observer.observe(card);
    });
}

/**
 * User feedback system
 */
function initializeUserFeedback() {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
}

/**
 * Utility functions
 */

/**
 * Show loading state on buttons
 */
function showButtonLoading(button, loadingText = 'Carregando...') {
    const originalText = button.textContent;
    const originalHtml = button.innerHTML;
    
    button.disabled = true;
    button.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        ${loadingText}
    `;
    
    return () => {
        button.disabled = false;
        button.innerHTML = originalHtml;
    };
}

/**
 * Show toast notification
 */
function showToast(message, type = 'info', duration = 3000) {
    const toastContainer = getOrCreateToastContainer();
    
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    
    const bsToast = new bootstrap.Toast(toast, { delay: duration });
    bsToast.show();
    
    // Remove toast from DOM after it's hidden
    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
}

function getOrCreateToastContainer() {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1055';
        document.body.appendChild(container);
    }
    return container;
}

/**
 * Smooth scroll to element
 */
function smoothScrollTo(element) {
    if (typeof element === 'string') {
        element = document.querySelector(element);
    }
    
    if (element) {
        element.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }
}

/**
 * Format date for display
 */
function formatDate(date, options = {}) {
    const defaultOptions = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    
    return new Date(date).toLocaleDateString('pt-BR', { ...defaultOptions, ...options });
}

/**
 * Debounce function for performance
 */
function debounce(func, wait, immediate) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            timeout = null;
            if (!immediate) func.apply(this, args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(this, args);
    };
}

// Export functions to global scope for use in templates
window.Inscrepper = {
    showButtonLoading,
    showToast,
    smoothScrollTo,
    formatDate,
    debounce
};
