/* script.js */
document.addEventListener('DOMContentLoaded', () => {
    // Səhifə yükləndikdə localStorage-dən mövzunu oxuyuruq
    const savedTheme = localStorage.getItem('theme');
    const body = document.body;

    // Əgər qaranlıq rejim yadda saxlanılıbsa, dark-mode sinfini əlavə edirik
    if (savedTheme === 'dark') {
        body.classList.add('dark-mode');
    }

    // Düymə vasitəsilə dəyişdirmək üçün funksiya
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', () => {
            body.classList.toggle('dark-mode');

            // Yeni seçimi localStorage-də yadda saxlayırıq
            if (body.classList.contains('dark-mode')) {
                localStorage.setItem('theme', 'dark');
            } else {
                localStorage.setItem('theme', 'light');
            }
        });
    }
});

/* --- Custom HTML Select Logic --- */
document.addEventListener('DOMContentLoaded', function () {
    // Open/Close dropdown
    document.addEventListener('click', function (e) {
        const trigger = e.target.closest('.custom-select-trigger');
        const option = e.target.closest('.custom-option');

        // If an option was clicked
        if (option) {
            const optionsList = option.closest('.custom-options');
            if (optionsList) {
                const listId = optionsList.id;
                const wrapper = document.querySelector('[data-dropdown-id="' + listId + '"]');
                if (wrapper) {
                    const hiddenInput = wrapper.querySelector('.custom-select-input');
                    const triggerText = wrapper.querySelector('.selected-text');
                    const allOptions = optionsList.querySelectorAll('.custom-option');

                    // Remove active classes
                    allOptions.forEach(opt => {
                        opt.classList.remove('bg-[#1e5631]', 'text-white');
                        opt.classList.remove('selected-option');
                    });

                    // Add active class
                    option.classList.add('bg-[#1e5631]', 'text-white', 'selected-option');

                    // Update values
                    const newValue = option.getAttribute('data-value');
                    if (hiddenInput) hiddenInput.value = newValue;
                    if (triggerText) triggerText.textContent = option.textContent;

                    // Close dropdown
                    optionsList.classList.add('hidden');
                    const t = wrapper.querySelector('.custom-select-trigger');
                    if (t) t.classList.remove('ring-2', 'ring-[#1e5631]');

                    // Dispatch event
                    if (hiddenInput) {
                        hiddenInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                }
            }
            return;
        }

        // Close all dropdowns if click is outside
        if (!trigger) {
            document.querySelectorAll('.custom-options').forEach(list => list.classList.add('hidden'));
            document.querySelectorAll('.custom-select-trigger').forEach(t => t.classList.remove('ring-2', 'ring-[#1e5631]'));
            return;
        }

        if (trigger) {
            const wrapper = trigger.closest('.custom-select-wrapper');
            if (!wrapper) return;

            // Check if disabled
            if (wrapper.style.pointerEvents === 'none' || wrapper.classList.contains('disabled')) {
                return;
            }

            let optionsList = wrapper.querySelector('.custom-options');

            // Move optionsList to body to avoid overflow hidden clipping
            const listId = wrapper.getAttribute('data-dropdown-id');
            if (listId) {
                optionsList = document.getElementById(listId);
            } else if (optionsList) {
                const newId = 'dropdown-' + Math.random().toString(36).substr(2, 9);
                wrapper.setAttribute('data-dropdown-id', newId);
                optionsList.id = newId;
                document.body.appendChild(optionsList);
            }

            // Close others
            document.querySelectorAll('.custom-options').forEach(list => {
                if (list && list.id !== (optionsList ? optionsList.id : '')) {
                    list.classList.add('hidden');
                }
            });
            document.querySelectorAll('.custom-select-trigger').forEach(t => {
                if (t !== trigger) t.classList.remove('ring-2', 'ring-[#1e5631]');
            });

            // Toggle current
            if (optionsList) {
                optionsList.classList.toggle('hidden');

                if (!optionsList.classList.contains('hidden')) {
                    trigger.classList.add('ring-2', 'ring-[#1e5631]');

                    // Position calculation
                    const rect = trigger.getBoundingClientRect();
                    optionsList.style.position = 'fixed';
                    optionsList.style.overscrollBehaviorY = 'contain';
                    optionsList.style.minWidth = rect.width + 'px';
                    optionsList.style.width = 'max-content';
                    optionsList.style.zIndex = '105000'; // high z-index for modals

                    // Temporarily show to get width/height
                    optionsList.style.visibility = 'hidden';
                    optionsList.style.display = 'block';
                    const listWidth = optionsList.offsetWidth;
                    const listHeight = optionsList.offsetHeight;
                    optionsList.style.display = '';
                    optionsList.style.visibility = '';

                    // Horizontal align - Expand left if near right edge
                    if (rect.left + listWidth > window.innerWidth) {
                        optionsList.style.right = (window.innerWidth - rect.right) + 'px';
                        optionsList.style.left = 'auto';
                    } else {
                        optionsList.style.left = rect.left + 'px';
                        optionsList.style.right = 'auto';
                    }

                    // Vertical align
                    const spaceBelow = window.innerHeight - rect.bottom;
                    const spaceAbove = rect.top;

                    if (spaceBelow < listHeight && spaceAbove > spaceBelow) {
                        // Show above
                        optionsList.style.top = (rect.top - listHeight) + 'px';
                    } else {
                        // Show below
                        optionsList.style.top = rect.bottom + 'px';
                    }
                } else {
                    trigger.classList.remove('ring-2', 'ring-[#1e5631]');
                }
            }
        }
    });

    // Close dropdowns on scroll to prevent detached floating menus
    // Siyahının özünü sürüşdürəndə onu açıq saxla.
    document.addEventListener('scroll', function (event) {
        if (event.target instanceof Element
            && event.target.closest('.custom-options')) {
            return;
        }

        document.querySelectorAll('.custom-options:not(.hidden)')
            .forEach(list => list.classList.add('hidden'));

        document.querySelectorAll('.custom-select-trigger')
            .forEach(t => t.classList.remove('ring-2', 'ring-[#1e5631]'));
    }, true);

    // Close on window resize
    window.addEventListener('resize', function () {
        document.querySelectorAll('.custom-options:not(.hidden)').forEach(list => list.classList.add('hidden'));
        document.querySelectorAll('.custom-select-trigger').forEach(t => t.classList.remove('ring-2', 'ring-[#1e5631]'));
    });
});

/* --- Miqdar İdarəetmə Funksiyaları --- */
function decreaseQty(btn) {
    if (!btn || btn.disabled) return;
    const container = btn.closest('.qty-container') || btn.parentElement;
    const input = container ? container.querySelector('input[name="quantity"]') : null;
    if (!input || input.disabled) return;
    const min = parseInt(input.min, 10) || 1;
    let val = parseInt(input.value, 10);
    if (isNaN(val)) val = min;
    if (val > min) {
        input.value = val - 1;
    }
    checkQtyInput(input);
}

function increaseQty(btn) {
    if (!btn || btn.disabled) return;
    const container = btn.closest('.qty-container') || btn.parentElement;
    const input = container ? container.querySelector('input[name="quantity"]') : null;
    if (!input || input.disabled) return;
    const min = parseInt(input.min, 10) || 1;
    const max = parseInt(input.max, 10);
    let val = parseInt(input.value, 10);
    if (isNaN(val)) val = min;
    if (!isNaN(max) && max > 0 && val >= max) {
        // Maksimum həddi keçmir
        checkQtyInput(input);
        return;
    }
    input.value = val + 1;
    checkQtyInput(input);
}

function checkQtyInput(input) {
    if (!input) return true;
    const min = parseInt(input.min, 10) || 1;
    const max = parseInt(input.max, 10);
    const val = parseInt(input.value, 10);

    input.setCustomValidity("");

    if (isNaN(val)) {
        input.setCustomValidity("Zəhmət olmasa düzgün say daxil edin!");
        input.reportValidity();
        return false;
    }
    if (val < min) {
        input.setCustomValidity("Miqdar ən azı " + min + " olmalıdır!");
        input.reportValidity();
        return false;
    }
    if (!isNaN(max) && max > 0 && val > max) {
        input.setCustomValidity("Tələb olunan miqdar (" + val + ") mövcud stokdan (" + max + ") çox ola bilməz!");
        input.reportValidity();
        return false;
    }
    return true;
}

function validateCartQuantity(form) {
    if (!form) return true;
    const input = form.querySelector('input[name="quantity"]');
    if (!input) return true;
    const valid = checkQtyInput(input);
    if (!valid) {
        input.reportValidity();
        return false;
    }
    return true;
}
