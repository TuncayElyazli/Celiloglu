import re

with open(r'C:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Remove the top Filters block completely
filters_pattern = r' *<!-- Filters -->.*?</form>\s*</div>\s*</div>'
html = re.sub(filters_pattern, '', html, flags=re.DOTALL)

# 2. Find the Main Content start
main_start_pattern = r'(<!-- Main Content -->\s*<main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">)'

new_main_start = """    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col lg:flex-row gap-8">

        <!-- Left Sidebar (Filters) -->
        <aside class="w-full lg:w-1/4 xl:w-1/5 flex-shrink-0">
            <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-gray-200 dark:border-white/10 p-5 sticky top-24 transition-colors duration-300">
                <h3 class="font-bold text-lg mb-6 text-gray-900 dark:text-white">Ağıllı Filtrləmə</h3>
                
                <form id="smartFilterForm" class="space-y-6">
                    <!-- Categories -->
                    <div>
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">Kateqoriyalar</h4>
                        <div class="space-y-3">
                            <label class="flex items-center space-x-3 cursor-pointer group">
                                <input type="checkbox" class="form-checkbox h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary focus:ring-2 bg-gray-50 dark:bg-slate-800 dark:border-gray-600 transition duration-200">
                                <span class="text-sm text-gray-600 dark:text-gray-300 group-hover:text-primary dark:group-hover:text-white transition-colors">Mühərrik (12)</span>
                            </label>
                            <label class="flex items-center space-x-3 cursor-pointer group">
                                <input type="checkbox" class="form-checkbox h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary focus:ring-2 bg-gray-50 dark:bg-slate-800 dark:border-gray-600 transition duration-200">
                                <span class="text-sm text-gray-600 dark:text-gray-300 group-hover:text-primary dark:group-hover:text-white transition-colors">Asqı sistemi (8)</span>
                            </label>
                            <label class="flex items-center space-x-3 cursor-pointer group">
                                <input type="checkbox" class="form-checkbox h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary focus:ring-2 bg-gray-50 dark:bg-slate-800 dark:border-gray-600 transition duration-200">
                                <span class="text-sm text-gray-600 dark:text-gray-300 group-hover:text-primary dark:group-hover:text-white transition-colors">Əyləc sistemi (15)</span>
                            </label>
                            <label class="flex items-center space-x-3 cursor-pointer group">
                                <input type="checkbox" class="form-checkbox h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary focus:ring-2 bg-gray-50 dark:bg-slate-800 dark:border-gray-600 transition duration-200">
                                <span class="text-sm text-gray-600 dark:text-gray-300 group-hover:text-primary dark:group-hover:text-white transition-colors">Elektrik (5)</span>
                            </label>
                        </div>
                    </div>

                    <hr class="border-gray-200 dark:border-white/10">

                    <!-- Price Range -->
                    <div>
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">Qiymət aralığı (AZN)</h4>
                        <div class="flex items-center gap-3">
                            <input type="number" placeholder="Min" class="w-full px-3 py-2 text-sm border border-gray-200 dark:border-white/20 bg-gray-50 dark:bg-white/10 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 dark:focus:ring-white/50 text-gray-800 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-colors">
                            <span class="text-gray-400">-</span>
                            <input type="number" placeholder="Max" class="w-full px-3 py-2 text-sm border border-gray-200 dark:border-white/20 bg-gray-50 dark:bg-white/10 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 dark:focus:ring-white/50 text-gray-800 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-colors">
                        </div>
                    </div>

                    <hr class="border-gray-200 dark:border-white/10">

                    <!-- Brand -->
                    <div>
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">Avtomobil markası</h4>
                        <div class="relative">
                            <select id="brandFilter" class="appearance-none block w-full pl-3 pr-10 py-2.5 text-sm font-medium border border-gray-200 dark:border-white/20 bg-gray-50 dark:bg-white/10 focus:outline-none focus:ring-2 focus:ring-primary/20 rounded-xl transition-all text-gray-700 dark:text-white [&>option]:text-gray-800">
                                <option value="all">Bütün markalar</option>
                                <option value="Toyota">Toyota</option>
                                <option value="Mercedes">Mercedes</option>
                                <option value="BMW">BMW</option>
                                <option value="Kia">Kia</option>
                                <option value="Hyundai">Hyundai</option>
                                <option value="Ford">Ford</option>
                                <option value="Honda">Honda</option>
                            </select>
                            <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-gray-500 dark:text-indigo-200">
                                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                                </svg>
                            </div>
                        </div>
                    </div>

                    <!-- Year -->
                    <div>
                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-3">İstehsal ili</h4>
                        <div class="relative">
                            <select id="yearFilter" class="appearance-none block w-full pl-3 pr-10 py-2.5 text-sm font-medium border border-gray-200 dark:border-white/20 bg-gray-50 dark:bg-white/10 focus:outline-none focus:ring-2 focus:ring-primary/20 rounded-xl transition-all text-gray-700 dark:text-white [&>option]:text-gray-800">
                                <option value="all">Bütün illər</option>
                                <option value="2015-2016">2015-2016</option>
                                <option value="2017-2018">2017-2018</option>
                                <option value="2019-2020">2019-2020</option>
                                <option value="2021-2022">2021-2022</option>
                                <option value="2023-2024">2023-2024</option>
                            </select>
                            <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-gray-500 dark:text-indigo-200">
                                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                                </svg>
                            </div>
                        </div>
                    </div>

                    <button type="button" class="w-full py-3 px-4 bg-primary hover:bg-primary/90 text-white font-bold rounded-xl shadow-[0_0_15px_rgba(99,102,241,0.4)] transition-all flex items-center justify-center gap-2 mt-4">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path></svg>
                        Filtrlə
                    </button>
                </form>
            </div>
        </aside>

        <!-- Right Content -->
        <div class="w-full lg:w-3/4 xl:w-4/5 flex-1">"""

html = re.sub(main_start_pattern, new_main_start, html, count=1)

# 3. Fix the h2 tag text-white
h2_pattern = r'<h2 class="text-2xl font-bold text-white">Ehtiyat hissələri \(Matrix\)</h2>'
html = re.sub(h2_pattern, '<h2 class="text-2xl font-bold text-gray-900 dark:text-white">Ehtiyat hissələri (Matrix)</h2>', html, count=1)

# 4. Change grid cols
grid_pattern = r'class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6"'
html = re.sub(grid_pattern, 'class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"', html, count=1)

# 5. Add closing div before </main>
main_end_pattern = r'(</main>)'
html = re.sub(main_end_pattern, r'</div>\n    \1', html, count=1)

with open(r'C:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\index.html', 'w', encoding='utf-8') as f:
    f.write(html)
print("Done")
