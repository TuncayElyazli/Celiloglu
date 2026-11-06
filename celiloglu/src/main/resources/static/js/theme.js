
(function () {
    function initTheme() {
        var themeToggleDarkIcon = document.getElementById('theme-toggle-dark-icon');
        var themeToggleLightIcon = document.getElementById('theme-toggle-light-icon');
        var themeToggleBtn = document.getElementById('theme-toggle');

        var isDark = localStorage.getItem('color-theme') === 'dark' ||
            (!('color-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);

        // Synchronize classes
        if (isDark) {
            document.documentElement.classList.add('dark');
            document.documentElement.classList.remove('light-mode');
            if (document.body) document.body.classList.remove('light-mode');
            if (themeToggleLightIcon) themeToggleLightIcon.classList.remove('hidden');
            if (themeToggleDarkIcon) themeToggleDarkIcon.classList.add('hidden');
        } else {
            document.documentElement.classList.remove('dark');
            document.documentElement.classList.add('light-mode');
            if (document.body) document.body.classList.add('light-mode');
            if (themeToggleDarkIcon) themeToggleDarkIcon.classList.remove('hidden');
            if (themeToggleLightIcon) themeToggleLightIcon.classList.add('hidden');
        }

        if (themeToggleBtn && !themeToggleBtn.dataset.themeBound) {
            themeToggleBtn.dataset.themeBound = 'true';
            themeToggleBtn.addEventListener('click', function () {
                var currentIsDark = document.documentElement.classList.contains('dark');
                if (currentIsDark) {
                    document.documentElement.classList.remove('dark');
                    document.documentElement.classList.add('light-mode');
                    if (document.body) document.body.classList.add('light-mode');
                    localStorage.setItem('color-theme', 'light');
                    if (themeToggleDarkIcon) themeToggleDarkIcon.classList.remove('hidden');
                    if (themeToggleLightIcon) themeToggleLightIcon.classList.add('hidden');
                } else {
                    document.documentElement.classList.add('dark');
                    document.documentElement.classList.remove('light-mode');
                    if (document.body) document.body.classList.remove('light-mode');
                    localStorage.setItem('color-theme', 'dark');
                    if (themeToggleLightIcon) themeToggleLightIcon.classList.remove('hidden');
                    if (themeToggleDarkIcon) themeToggleDarkIcon.classList.add('hidden');
                }
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTheme);
    } else {
        initTheme();
    }
})();
