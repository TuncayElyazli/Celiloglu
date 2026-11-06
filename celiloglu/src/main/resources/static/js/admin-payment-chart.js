/**
 * Admin Dashboard Payment Dynamics Interactive Chart (Chart.js)
 */
(function () {
    'use strict';

    function initChart() {
        const canvas = document.getElementById('payment-chart-canvas');
        if (!canvas) return;

        // Check if Chart.js is loaded
        if (typeof Chart === 'undefined') {
            console.warn('Chart.js not loaded, keeping SVG fallback view.');
            return;
        }

        const rawData = window.__paymentChartDailyData;
        if (!Array.isArray(rawData) || rawData.length === 0) {
            console.info('No payment data available for chart.');
            return;
        }

        // DOM elements
        const canvasContainer = document.getElementById('payment-chart-canvas-container');
        const svgFallback = document.getElementById('payment-chart-svg-fallback');
        const controls = document.getElementById('payment-chart-controls');
        const typeControls = document.getElementById('payment-chart-type-controls');
        const btnPeriod7 = document.getElementById('btn-period-7');
        const btnPeriod30 = document.getElementById('btn-period-30');
        const btnTypeLine = document.getElementById('btn-type-line');
        const btnTypeBar = document.getElementById('btn-type-bar');
        const periodLabel = document.getElementById('payment-chart-period-label');
        const periodSubtitle = document.getElementById('payment-chart-period-subtitle');
        const periodTotal = document.getElementById('payment-chart-period-total');
        const noDataNotice = document.getElementById('payment-chart-no-data-notice');

        // State
        let currentPeriod = 30; // 30 or 7
        let currentType = 'line'; // 'line' or 'bar'
        let chartInstance = null;

        function isDarkTheme() {
            return document.documentElement.classList.contains('dark');
        }

        function getActiveData() {
            if (currentPeriod === 7) {
                return rawData.slice(-7);
            }
            return rawData;
        }

        function calculateCentsSum(dataPoints) {
            let cents = 0;
            for (let i = 0; i < dataPoints.length; i++) {
                const amt = Number(dataPoints[i].amount) || 0;
                cents += Math.round(amt * 100);
            }
            return cents;
        }

        function formatMoney(cents) {
            return (cents / 100).toFixed(2) + ' AZN';
        }

        function updateSummaryUI() {
            const data = getActiveData();
            const cents = calculateCentsSum(data);

            if (periodLabel) {
                periodLabel.textContent = currentPeriod + ' Günün Cəmi';
            }
            if (periodSubtitle) {
                periodSubtitle.textContent = currentPeriod;
            }
            if (periodTotal) {
                periodTotal.textContent = formatMoney(cents);
            }
            if (noDataNotice) {
                if (cents === 0) {
                    noDataNotice.classList.remove('hidden');
                } else {
                    noDataNotice.classList.add('hidden');
                }
            }
        }

        function updateButtonUI() {
            const activePeriodClasses = ['bg-white', 'dark:bg-[#1e5631]', 'text-primary', 'dark:text-white', 'shadow-sm'];
            const inactivePeriodClasses = ['text-gray-600', 'dark:text-gray-400', 'hover:text-gray-900', 'dark:hover:text-white'];

            if (btnPeriod7 && btnPeriod30) {
                if (currentPeriod === 7) {
                    activePeriodClasses.forEach(cls => btnPeriod7.classList.add(cls));
                    inactivePeriodClasses.forEach(cls => btnPeriod7.classList.remove(cls));
                    activePeriodClasses.forEach(cls => btnPeriod30.classList.remove(cls));
                    inactivePeriodClasses.forEach(cls => btnPeriod30.classList.add(cls));
                } else {
                    activePeriodClasses.forEach(cls => btnPeriod30.classList.add(cls));
                    inactivePeriodClasses.forEach(cls => btnPeriod30.classList.remove(cls));
                    activePeriodClasses.forEach(cls => btnPeriod7.classList.remove(cls));
                    inactivePeriodClasses.forEach(cls => btnPeriod7.classList.add(cls));
                }
            }

            if (btnTypeLine && btnTypeBar) {
                if (currentType === 'line') {
                    activePeriodClasses.forEach(cls => btnTypeLine.classList.add(cls));
                    inactivePeriodClasses.forEach(cls => btnTypeLine.classList.remove(cls));
                    activePeriodClasses.forEach(cls => btnTypeBar.classList.remove(cls));
                    inactivePeriodClasses.forEach(cls => btnTypeBar.classList.add(cls));
                } else {
                    activePeriodClasses.forEach(cls => btnTypeBar.classList.add(cls));
                    inactivePeriodClasses.forEach(cls => btnTypeBar.classList.remove(cls));
                    activePeriodClasses.forEach(cls => btnTypeLine.classList.remove(cls));
                    inactivePeriodClasses.forEach(cls => btnTypeLine.classList.add(cls));
                }
            }
        }

        function buildChartConfig() {
            const isDark = isDarkTheme();
            const data = getActiveData();

            const labels = data.map(pt => pt.formattedDate);
            const values = data.map(pt => Number(pt.amount) || 0);

            const brandColor = isDark ? '#2ecc71' : '#1e5631';
            const gridColor = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.06)';
            const textColor = isDark ? '#9ca3af' : '#6b7280';

            const ctx = canvas.getContext('2d');
            let backgroundColor;

            if (currentType === 'line') {
                const gradient = ctx.createLinearGradient(0, 0, 0, 240);
                if (isDark) {
                    gradient.addColorStop(0, 'rgba(46, 204, 113, 0.28)');
                    gradient.addColorStop(1, 'rgba(46, 204, 113, 0.0)');
                } else {
                    gradient.addColorStop(0, 'rgba(30, 86, 49, 0.25)');
                    gradient.addColorStop(1, 'rgba(30, 86, 49, 0.0)');
                }
                backgroundColor = gradient;
            } else {
                backgroundColor = isDark ? 'rgba(46, 204, 113, 0.75)' : 'rgba(30, 86, 49, 0.75)';
            }

            return {
                type: currentType,
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Daxil olan ödəniş',
                        data: values,
                        borderColor: brandColor,
                        borderWidth: currentType === 'line' ? 2.5 : 0,
                        backgroundColor: backgroundColor,
                        fill: currentType === 'line',
                        tension: 0.35,
                        borderRadius: currentType === 'bar' ? 6 : 0,
                        borderSkipped: false,
                        pointBackgroundColor: brandColor,
                        pointBorderColor: isDark ? '#1e1e1e' : '#ffffff',
                        pointBorderWidth: 2,
                        pointRadius: currentType === 'line' ? (currentPeriod === 7 ? 5 : 3.5) : 0,
                        pointHoverRadius: currentType === 'line' ? 6.5 : 0,
                        pointHoverBackgroundColor: brandColor,
                        pointHoverBorderColor: '#ffffff',
                        pointHoverBorderWidth: 2.5
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    },
                    animation: {
                        duration: 400
                    },
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            backgroundColor: isDark ? '#1e1e1e' : '#ffffff',
                            titleColor: isDark ? '#f3f4f6' : '#111827',
                            bodyColor: isDark ? '#e5e7eb' : '#374151',
                            borderColor: isDark ? '#374151' : '#e5e7eb',
                            borderWidth: 1,
                            padding: 10,
                            boxPadding: 4,
                            usePointStyle: true,
                            callbacks: {
                                title: function (tooltipItems) {
                                    if (!tooltipItems || tooltipItems.length === 0) return '';
                                    const idx = tooltipItems[0].dataIndex;
                                    return (data[idx] && data[idx].fullDate) ? data[idx].fullDate : tooltipItems[0].label;
                                },
                                label: function (context) {
                                    const val = Number(context.parsed.y) || 0;
                                    return ' Ödəniş: ' + val.toFixed(2) + ' AZN';
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            grid: {
                                display: false
                            },
                            ticks: {
                                color: textColor,
                                font: {
                                    size: 11,
                                    weight: '500'
                                },
                                autoSkip: true,
                                maxTicksLimit: currentPeriod === 7 ? 7 : 8,
                                maxRotation: 0
                            }
                        },
                        y: {
                            beginAtZero: true,
                            grid: {
                                color: gridColor,
                                drawBorder: false
                            },
                            ticks: {
                                color: textColor,
                                font: {
                                    size: 11
                                },
                                callback: function (val) {
                                    if (val >= 1000) {
                                        return (val / 1000).toFixed(1) + 'k AZN';
                                    }
                                    return val + ' AZN';
                                }
                            }
                        }
                    }
                }
            };
        }

        function renderChart() {
            if (chartInstance) {
                chartInstance.destroy();
                chartInstance = null;
            }
            const config = buildChartConfig();
            chartInstance = new Chart(canvas, config);
        }

        // Initialize: show interactive canvas & controls, hide SVG fallback
        try {
            renderChart();

            if (canvasContainer) canvasContainer.classList.remove('hidden');
            if (controls) controls.classList.remove('hidden');
            if (typeControls) typeControls.classList.remove('hidden');
            if (svgFallback) svgFallback.classList.add('hidden');

            updateSummaryUI();
            updateButtonUI();
        } catch (err) {
            console.error('Error initializing payment Chart.js instance:', err);
            // On error, ensure SVG fallback remains visible
            if (svgFallback) svgFallback.classList.remove('hidden');
            if (canvasContainer) canvasContainer.classList.add('hidden');
            return;
        }

        // Bind Controls (once only)
        if (!canvas.dataset.bound) {
            canvas.dataset.bound = 'true';

            if (btnPeriod7) {
                btnPeriod7.addEventListener('click', function () {
                    if (currentPeriod === 7) return;
                    currentPeriod = 7;
                    updateButtonUI();
                    updateSummaryUI();
                    renderChart();
                });
            }

            if (btnPeriod30) {
                btnPeriod30.addEventListener('click', function () {
                    if (currentPeriod === 30) return;
                    currentPeriod = 30;
                    updateButtonUI();
                    updateSummaryUI();
                    renderChart();
                });
            }

            if (btnTypeLine) {
                btnTypeLine.addEventListener('click', function () {
                    if (currentType === 'line') return;
                    currentType = 'line';
                    updateButtonUI();
                    renderChart();
                });
            }

            if (btnTypeBar) {
                btnTypeBar.addEventListener('click', function () {
                    if (currentType === 'bar') return;
                    currentType = 'bar';
                    updateButtonUI();
                    renderChart();
                });
            }

            // Theme change observer
            const themeObserver = new MutationObserver(function (mutations) {
                for (let i = 0; i < mutations.length; i++) {
                    if (mutations[i].attributeName === 'class') {
                        if (chartInstance) {
                            renderChart();
                        }
                        break;
                    }
                }
            });
            themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });

            // Resize observer for tab switching and window resizing
            if (window.ResizeObserver && canvasContainer) {
                const resizeObserver = new ResizeObserver(function () {
                    if (chartInstance && canvasContainer.offsetParent !== null) {
                        chartInstance.resize();
                    }
                });
                resizeObserver.observe(canvasContainer);
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChart);
    } else {
        initChart();
    }
})();

