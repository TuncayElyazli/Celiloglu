const fs = require('fs');
const path = require('path');

function updateFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf-8');

    // 1. Update tailwind config to include darkMode: 'class'
    content = content.replace(
        "tailwind.config = {\n            theme: {",
        "tailwind.config = {\n            darkMode: 'class',\n            theme: {"
    );

    // 2. Add inline dark mode check to head
    const headScript = `    <script>
        if (localStorage.getItem('color-theme') === 'dark' || (!('color-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    </script>`;
    content = content.replace("</script>\n    <style>", "</script>\n" + headScript + "\n    <style>");

    // 3. Add dark mode classes to body
    content = content.replace(
        '<body class="flex h-screen overflow-hidden text-gray-800">',
        '<body class="flex h-screen overflow-hidden text-gray-800 dark:text-gray-200 bg-gray-50 dark:bg-slate-900 transition-colors">'
    );

    // 4. Sidebar update
    content = content.replace(
        '<aside class="w-64 bg-sidebar text-white flex flex-col hidden md:flex flex-shrink-0 shadow-xl z-20">',
        '<aside class="w-64 bg-sidebar dark:bg-slate-950 text-white flex flex-col hidden md:flex flex-shrink-0 shadow-xl z-20 border-r border-transparent dark:border-white/5 transition-colors">'
    );

    // 5. Main Wrapper bg update
    content = content.replace(
        '<div class="flex-1 flex flex-col h-screen overflow-hidden bg-gray-50">',
        '<div class="flex-1 flex flex-col h-screen overflow-hidden bg-gray-50 dark:bg-slate-900 transition-colors">'
    );

    // 6. Top Header updates + Add Toggle button
    const headerOld = `        <header
            class="h-20 bg-white border-b border-gray-200 flex items-center justify-between px-8 flex-shrink-0 z-10">
            <h1 class="text-xl font-bold text-gray-800">Ümumi Baxış</h1>
            <div class="flex items-center gap-4">
                <div class="text-right hidden sm:block">
                    <p class="text-sm font-bold text-gray-900">Admin İstifadeçi</p>
                    <p class="text-xs text-gray-500 font-medium">Super Admin</p>
                </div>
                <div
                    class="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center text-primary border border-primary/20">`;
                    
    const headerNew = `        <header
            class="h-20 bg-white dark:bg-slate-900/80 backdrop-blur-md border-b border-gray-200 dark:border-white/10 flex items-center justify-between px-8 flex-shrink-0 z-10 transition-colors">
            <h1 class="text-xl font-bold text-gray-800 dark:text-white">Ümumi Baxış</h1>
            <div class="flex items-center gap-6">
                <!-- Theme Toggle -->
                <button id="theme-toggle" type="button"
                    class="text-gray-500 dark:text-indigo-100 hover:text-primary dark:hover:text-white flex flex-col items-center transition-all duration-300 group dark:hover:drop-shadow-[0_0_10px_rgba(255,255,255,0.8)] focus:outline-none">
                    <svg id="theme-toggle-dark-icon" class="hidden w-5 h-5 group-hover:scale-110 transition-transform" fill="currentColor"
                        viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                        <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"></path>
                    </svg>
                    <svg id="theme-toggle-light-icon" class="hidden w-5 h-5 group-hover:scale-110 group-hover:rotate-90 transition-all duration-500" fill="currentColor"
                        viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                        <path
                            d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"
                            fill-rule="evenodd" clip-rule="evenodd"></path>
                    </svg>
                </button>
                <div class="text-right hidden sm:block">
                    <p class="text-sm font-bold text-gray-900 dark:text-white">Admin İstifadeçi</p>
                    <p class="text-xs text-gray-500 dark:text-gray-400 font-medium">Super Admin</p>
                </div>
                <div
                    class="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center text-primary border border-primary/20 dark:border-white/10 dark:bg-white/5 dark:text-indigo-300">`;
    content = content.replace(headerOld, headerNew);

    // 7. Dashboard Stats Updates
    content = content.split('class="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm flex items-center gap-4"').join('class="bg-white dark:bg-indigo-900/30 rounded-2xl p-6 border border-gray-100 dark:border-white/10 shadow-sm dark:shadow-[0_0_15px_rgba(255,255,255,0.05)] dark:backdrop-blur-sm flex items-center gap-4 transition-colors"');
    content = content.split('class="text-2xl font-black text-gray-900"').join('class="text-2xl font-black text-gray-900 dark:text-white"');
    content = content.split('class="text-sm font-semibold text-gray-500 mb-1 uppercase tracking-wider"').join('class="text-sm font-semibold text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wider"');
    
    // Icons bg inside stats
    content = content.split('bg-blue-50').join('bg-blue-50 dark:bg-blue-900/30');
    content = content.split('text-blue-600').join('text-blue-600 dark:text-blue-400');
    content = content.split('bg-purple-50').join('bg-purple-50 dark:bg-purple-900/30');
    content = content.split('text-purple-600').join('text-purple-600 dark:text-purple-400');
    content = content.split('bg-green-50').join('bg-green-50 dark:bg-green-900/30');
    content = content.split('text-green-600').join('text-green-600 dark:text-green-400');

    // 8. Tables and Titles Updates
    content = content.split('class="text-lg font-bold text-gray-900"').join('class="text-lg font-bold text-gray-900 dark:text-white"');
    content = content.split('class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden"').join('class="bg-white dark:bg-indigo-900/30 rounded-2xl border border-gray-100 dark:border-white/10 shadow-sm dark:shadow-[0_0_15px_rgba(255,255,255,0.05)] dark:backdrop-blur-sm overflow-hidden transition-colors"');
    content = content.split('class="bg-gray-50/80 border-b border-gray-100 text-xs font-semibold text-gray-500 uppercase tracking-wider"').join('class="bg-gray-50/80 dark:bg-white/5 border-b border-gray-100 dark:border-white/10 text-xs font-semibold text-gray-500 dark:text-gray-300 uppercase tracking-wider transition-colors"');
    content = content.split('class="divide-y divide-gray-100 text-sm"').join('class="divide-y divide-gray-100 dark:divide-white/10 text-sm"');
    content = content.split('class="hover:bg-gray-50/50 transition-colors"').join('class="hover:bg-gray-50/50 dark:hover:bg-white/5 transition-colors"');
    
    // Table texts
    content = content.split('class="px-6 py-4 font-medium text-gray-900"').join('class="px-6 py-4 font-medium text-gray-900 dark:text-white"');
    content = content.split('class="px-6 py-4 text-gray-600"').join('class="px-6 py-4 text-gray-600 dark:text-gray-300"');
    content = content.split('class="px-6 py-4 text-gray-600 font-medium"').join('class="px-6 py-4 text-gray-600 dark:text-gray-300 font-medium"');
    content = content.split('class="px-6 py-4 font-bold text-gray-900"').join('class="px-6 py-4 font-bold text-gray-900 dark:text-white"');
    content = content.split('class="px-6 py-4 text-gray-500 max-w-xs truncate"').join('class="px-6 py-4 text-gray-500 dark:text-gray-400 max-w-xs truncate"');
    content = content.split('class="px-6 py-4 text-gray-700 text-sm"').join('class="px-6 py-4 text-gray-700 dark:text-gray-300 text-sm"');
    content = content.split('class="px-6 py-4 font-medium text-gray-700"').join('class="px-6 py-4 font-medium text-gray-700 dark:text-gray-300"');
    
    // Buttons in table
    content = content.split('class="p-2 text-secondary hover:bg-secondary/10 rounded-lg transition-colors border border-transparent hover:border-secondary/20"').join('class="p-2 text-secondary dark:text-indigo-400 hover:bg-secondary/10 dark:hover:bg-indigo-400/20 rounded-lg transition-colors border border-transparent hover:border-secondary/20"');
    content = content.split('class="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors border border-transparent hover:border-red-100"').join('class="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-500/20 rounded-lg transition-colors border border-transparent hover:border-red-100"');

    // Button Add product
    content = content.split('class="bg-gray-900 text-white text-sm font-semibold px-4 py-2.5 rounded-xl hover:bg-gray-800 transition-colors shadow-sm flex items-center gap-2 focus:ring-4 focus:ring-gray-200"').join('class="bg-gray-900 dark:bg-primary text-white text-sm font-semibold px-4 py-2.5 rounded-xl hover:bg-gray-800 dark:hover:bg-primary/90 transition-colors shadow-sm flex items-center gap-2 focus:ring-4 focus:ring-gray-200"');


    // Modals
    content = content.split('class="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity"').join('class="absolute inset-0 bg-black/50 dark:bg-slate-900/80 backdrop-blur-sm transition-opacity"');
    content = content.split('class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden flex flex-col max-h-[90vh]"').join('class="relative bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden flex flex-col max-h-[90vh] dark:border dark:border-white/10"');
    content = content.split('class="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50"').join('class="px-6 py-4 border-b border-gray-100 dark:border-white/10 flex justify-between items-center bg-gray-50/50 dark:bg-slate-800/50"');
    content = content.split('class="px-6 py-4 border-t border-gray-100 flex justify-end gap-3 bg-gray-50/50"').join('class="px-6 py-4 border-t border-gray-100 dark:border-white/10 flex justify-end gap-3 bg-gray-50/50 dark:bg-slate-800/50"');
    
    content = content.split('class="text-lg font-bold text-gray-900"').join('class="text-lg font-bold text-gray-900 dark:text-white"');
    content = content.split('class="block text-sm font-medium text-gray-700 mb-1"').join('class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"');
    content = content.split('class="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-sm file:font-semibold file:bg-primary/10 file:text-primary hover:file:bg-primary/20 transition-colors"').join('class="w-full text-sm text-gray-500 dark:text-gray-300 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-sm file:font-semibold file:bg-primary/10 file:text-primary hover:file:bg-primary/20 transition-colors"');
    content = content.split('class="w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors"').join('class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/20 dark:bg-slate-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors placeholder-gray-400"');
    content = content.split('class="w-full px-4 py-2.5 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors text-sm text-gray-700"').join('class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/20 dark:bg-slate-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-colors text-sm text-gray-700 dark:text-white"');
    content = content.split('class="px-5 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 transition-colors focus:ring-2 focus:ring-gray-200"').join('class="px-5 py-2.5 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-slate-700 border border-gray-300 dark:border-white/20 rounded-xl hover:bg-gray-50 dark:hover:bg-slate-600 transition-colors focus:ring-2 focus:ring-gray-200"');
    
    // 9. Inject Theme Toggle Logic at the end
    const scriptAppend = `
        // Theme toggle logic
        var themeToggleDarkIcon = document.getElementById('theme-toggle-dark-icon');
        var themeToggleLightIcon = document.getElementById('theme-toggle-light-icon');

        if (localStorage.getItem('color-theme') === 'dark' || (!('color-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            themeToggleLightIcon.classList.remove('hidden');
        } else {
            themeToggleDarkIcon.classList.remove('hidden');
        }

        var themeToggleBtn = document.getElementById('theme-toggle');

        if(themeToggleBtn) {
            themeToggleBtn.addEventListener('click', function () {
                themeToggleDarkIcon.classList.toggle('hidden');
                themeToggleLightIcon.classList.toggle('hidden');

                if (localStorage.getItem('color-theme')) {
                    if (localStorage.getItem('color-theme') === 'light') {
                        document.documentElement.classList.add('dark');
                        localStorage.setItem('color-theme', 'dark');
                    } else {
                        document.documentElement.classList.remove('dark');
                        localStorage.setItem('color-theme', 'light');
                    }
                } else {
                    if (document.documentElement.classList.contains('dark')) {
                        document.documentElement.classList.remove('dark');
                        localStorage.setItem('color-theme', 'light');
                    } else {
                        document.documentElement.classList.add('dark');
                        localStorage.setItem('color-theme', 'dark');
                    }
                }
            });
        }
    });
    </script>`;
    
    content = content.replace("    });\n        });\n    </script>", "    });\n    </script>");
    content = content.replace("    });\n    </script>", scriptAppend);

    fs.writeFileSync(filePath, content, 'utf-8');
}

updateFile(path.join(__dirname, '../src/main/resources/templates/admin.html'));
