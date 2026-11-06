$content = Get-Content -Path "C:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\index.html" -Raw
$parts = $content -split '<div class="product-card'
for ($i = 1; $i -lt $parts.Length; $i++) {
    $card = '<div class="product-card' + $parts[$i]
    if ($i -eq $parts.Length - 1) {
        # Keep everything except what's after pagination just in case
    }
    
    $divCount = ([regex]::Matches($card, '<div\b')).Count
    $endDivCount = ([regex]::Matches($card, '</div\b')).Count
    
    $code = "Unknown"
    if ($card -match 'data-code="([^"]+)"') {
        $code = $matches[1]
    }
    
    Write-Host "Card $i (Code: $code): $divCount divs, $endDivCount enddivs"
}
