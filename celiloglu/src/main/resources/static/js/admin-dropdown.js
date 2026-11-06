// admin-dropdown.js - Reusable Custom Accessible Dropdowns for Celiloglu Admin Panel
(function () {
    'use strict';

    function initAdminCustomDropdowns() {
        var selector = '.custom-dropdown-wrapper, .role-custom-select-wrapper';
        var wrappers = document.querySelectorAll(selector);
        if (!wrappers || wrappers.length === 0) return;

        wrappers.forEach(function (wrapper, wIdx) {
            if (wrapper.dataset.customDropdownInitialized === 'true') return;
            wrapper.dataset.customDropdownInitialized = 'true';

            var nativeSelect = wrapper.querySelector('.custom-dropdown-native, .role-native-select, select');
            var trigger = wrapper.querySelector('.custom-dropdown-trigger, .role-select-trigger');
            var label = wrapper.querySelector('.custom-dropdown-label, .role-select-label');
            var chevron = wrapper.querySelector('.custom-dropdown-chevron, .role-select-chevron');
            var menu = wrapper.querySelector('.custom-dropdown-menu, .role-select-menu');
            if (!nativeSelect || !trigger || !label || !chevron || !menu) return;

            var options = Array.from(wrapper.querySelectorAll('.custom-dropdown-option, .role-select-option'));
            var isOpen = false;
            var focusedIndex = -1;

            // Dynamic unique IDs for accessibility & ARIA binding
            var uid = 'admin-dd-' + wIdx + '-' + Math.random().toString(36).substring(2, 7);
            var triggerId = uid + '-btn';
            var menuId = uid + '-menu';
            trigger.id = triggerId;
            menu.id = menuId;
            trigger.setAttribute('aria-controls', menuId);
            menu.setAttribute('aria-labelledby', triggerId);

            options.forEach(function (opt, optIdx) {
                opt.id = uid + '-opt-' + optIdx;
            });

            // Progressive enhancement: reveal custom dropdown trigger, hide native select
            nativeSelect.classList.add('hidden');
            nativeSelect.setAttribute('aria-hidden', 'true');
            nativeSelect.setAttribute('tabindex', '-1');
            trigger.classList.remove('hidden');

            function getOptionData(opt) {
                var val = opt.getAttribute('data-value') || '';
                var titleEl = opt.querySelector('.custom-dropdown-opt-title, .role-opt-title');
                var subEl = opt.querySelector('.custom-dropdown-opt-sub, .role-opt-sub');
                var title = titleEl ? titleEl.textContent.trim() : opt.textContent.trim();
                var sub = subEl ? subEl.textContent.trim() : '';
                return { value: val, title: title, sub: sub };
            }

            function syncFromNativeSelect() {
                var currentVal = nativeSelect.value;
                var selectedOpt = null;

                options.forEach(function (opt) {
                    var val = opt.getAttribute('data-value');
                    var isSelected = (val === currentVal);
                    opt.setAttribute('aria-selected', isSelected ? 'true' : 'false');
                    var checkIcon = opt.querySelector('.custom-dropdown-check-icon, .role-check-icon');
                    if (checkIcon) {
                        if (isSelected) {
                            checkIcon.classList.remove('hidden');
                            opt.classList.add('bg-gray-100/70', 'dark:bg-white/10');
                        } else {
                            checkIcon.classList.add('hidden');
                            opt.classList.remove('bg-gray-100/70', 'dark:bg-white/10');
                        }
                    }
                    if (isSelected) {
                        selectedOpt = opt;
                    }
                });

                if (selectedOpt) {
                    var data = getOptionData(selectedOpt);
                    label.innerHTML = '<span class="font-bold text-gray-900 dark:text-white">' + data.title + '</span>' +
                        (data.sub ? ' <span class="text-[11px] text-gray-500 dark:text-gray-400 font-normal">(' + data.sub + ')</span>' : '');
                } else if (options.length > 0) {
                    var firstData = getOptionData(options[0]);
                    label.innerHTML = '<span class="font-bold text-gray-900 dark:text-white">' + firstData.title + '</span>' +
                        (firstData.sub ? ' <span class="text-[11px] text-gray-500 dark:text-gray-400 font-normal">(' + firstData.sub + ')</span>' : '');
                }
            }

            function openMenu() {
                if (isOpen) return;

                // Close any other open dropdowns first
                document.querySelectorAll('.custom-dropdown-wrapper, .role-custom-select-wrapper').forEach(function (otherWrapper) {
                    if (otherWrapper !== wrapper && typeof otherWrapper._closeAdminDropdown === 'function') {
                        otherWrapper._closeAdminDropdown(false);
                    }
                });

                // Smart vertical positioning: flip upward if near viewport bottom
                var rect = trigger.getBoundingClientRect();
                var viewportHeight = window.innerHeight || document.documentElement.clientHeight;
                var spaceBelow = viewportHeight - rect.bottom;
                var spaceAbove = rect.top;
                if (spaceBelow < 210 && spaceAbove > spaceBelow) {
                    menu.classList.remove('top-full', 'mt-1.5');
                    menu.classList.add('bottom-full', 'mb-1.5');
                } else {
                    menu.classList.remove('bottom-full', 'mb-1.5');
                    menu.classList.add('top-full', 'mt-1.5');
                }

                isOpen = true;
                wrapper._isOpen = true;
                menu.classList.remove('hidden');
                trigger.setAttribute('aria-expanded', 'true');
                chevron.classList.add('rotate-180');

                var curVal = nativeSelect.value;
                focusedIndex = options.findIndex(function (o) {
                    return o.getAttribute('data-value') === curVal;
                });
                if (focusedIndex === -1 && options.length > 0) focusedIndex = 0;
                highlightOption(focusedIndex);
            }

            function closeMenu(focusTrigger) {
                if (!isOpen) return;
                isOpen = false;
                wrapper._isOpen = false;
                menu.classList.add('hidden');
                trigger.setAttribute('aria-expanded', 'false');
                trigger.removeAttribute('aria-activedescendant');
                chevron.classList.remove('rotate-180');
                if (focusTrigger) {
                    trigger.focus();
                }
            }

            wrapper._closeAdminDropdown = closeMenu;
            wrapper._closeRoleMenu = closeMenu;
            wrapper._isOpen = false;

            function toggleMenu() {
                if (isOpen) {
                    closeMenu(true);
                } else {
                    openMenu();
                }
            }

            function highlightOption(index) {
                if (index < 0 || index >= options.length) return;
                focusedIndex = index;
                options[index].focus();
                trigger.setAttribute('aria-activedescendant', options[index].id);
            }

            function selectOption(opt) {
                var newVal = opt.getAttribute('data-value');
                if (newVal) {
                    nativeSelect.value = newVal;
                    nativeSelect.dispatchEvent(new Event('change', { bubbles: true }));
                    syncFromNativeSelect();
                }
                closeMenu(true);
            }

            // Trigger button events
            trigger.addEventListener('click', function (e) {
                e.preventDefault();
                toggleMenu();
            });

            trigger.addEventListener('keydown', function (e) {
                if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    if (!isOpen) {
                        openMenu();
                    } else if (e.key === 'ArrowDown') {
                        highlightOption((focusedIndex + 1) % options.length);
                    } else if (e.key === 'ArrowUp') {
                        highlightOption((focusedIndex - 1 + options.length) % options.length);
                    } else if (e.key === 'Enter' || e.key === ' ') {
                        if (focusedIndex >= 0 && focusedIndex < options.length) {
                            selectOption(options[focusedIndex]);
                        }
                    }
                } else if (e.key === 'Escape' && isOpen) {
                    e.preventDefault();
                    closeMenu(true);
                }
            });

            // Options events
            options.forEach(function (opt, idx) {
                opt.addEventListener('click', function (e) {
                    e.preventDefault();
                    selectOption(opt);
                });

                opt.addEventListener('mouseenter', function () {
                    focusedIndex = idx;
                });

                opt.addEventListener('keydown', function (e) {
                    if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        highlightOption((idx + 1) % options.length);
                    } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        highlightOption((idx - 1 + options.length) % options.length);
                    } else if (e.key === 'Home') {
                        e.preventDefault();
                        highlightOption(0);
                    } else if (e.key === 'End') {
                        e.preventDefault();
                        highlightOption(options.length - 1);
                    } else if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        selectOption(opt);
                    } else if (e.key === 'Escape') {
                        e.preventDefault();
                        closeMenu(true);
                    } else if (e.key === 'Tab') {
                        closeMenu(false);
                    }
                });
            });

            // Native select change sync (e.g. if reset or updated programmatically)
            nativeSelect.addEventListener('change', function () {
                syncFromNativeSelect();
            });

            // Initial sync
            syncFromNativeSelect();
        });

        // Global click outside & Escape listeners (registered once on document)
        if (!document._adminCustomDropdownGlobalBound) {
            document._adminCustomDropdownGlobalBound = true;
            document.addEventListener('click', function (e) {
                var openWrappers = document.querySelectorAll('.custom-dropdown-wrapper, .role-custom-select-wrapper');
                openWrappers.forEach(function (wrapper) {
                    if (wrapper._isOpen && !wrapper.contains(e.target)) {
                        if (typeof wrapper._closeAdminDropdown === 'function') {
                            wrapper._closeAdminDropdown(false);
                        }
                    }
                });
            });

            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') {
                    var openWrappers = document.querySelectorAll('.custom-dropdown-wrapper, .role-custom-select-wrapper');
                    openWrappers.forEach(function (wrapper) {
                        if (wrapper._isOpen) {
                            if (typeof wrapper._closeAdminDropdown === 'function') {
                                wrapper._closeAdminDropdown(true);
                            }
                        }
                    });
                }
            });
        }
    }

    window.initAdminCustomDropdowns = initAdminCustomDropdowns;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAdminCustomDropdowns);
    } else {
        initAdminCustomDropdowns();
    }
})();

