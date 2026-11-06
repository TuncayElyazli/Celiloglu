$files = @("c:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\index.html", "c:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\admin.html")
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

foreach ($file in $files) {
    $html = [System.IO.File]::ReadAllText($file, $utf8NoBom)
    
    # Replace <select> tags
    $regex = '(?is)<select([^>]+)>(.*?)</select>'
    $html = [regex]::Replace($html, $regex, {
        param($match)
        $attrs = $match.Groups[1].Value
        $optionsRaw = $match.Groups[2].Value
        
        $id = ""
        if ($attrs -match 'id="([^"]+)"') { $id = $matches[1] }
        
        $class = ""
        if ($attrs -match 'class="([^"]+)"') { $class = $matches[1] }
        
        $options = [regex]::Matches($optionsRaw, '(?is)<option\s+value="([^"]*)"([^>]*)>(.*?)</option>')
        
        $selectedText = ""
        $selectedValue = ""
        $lis = ""
        foreach ($opt in $options) {
            $val = $opt.Groups[1].Value
            $optAttrs = $opt.Groups[2].Value
            $text = $opt.Groups[3].Value
            
            $isSelected = $optAttrs -match "selected"
            if ($isSelected -or $selectedText -eq "") {
                $selectedText = $text
                $selectedValue = $val
            }
            $liClass = "custom-option px-4 py-2 hover:bg-[#1e5631] text-[#e0e0e0] cursor-pointer transition-colors"
            if ($isSelected) { $liClass += " bg-[#1e5631] text-white" }
            
            $lis += "<li class="$liClass" data-value="$val">$text</li>
"
        }
        
        $wrapperClass = "custom-select-wrapper relative "
        if ($class -match 'w-full') { $wrapperClass += "w-full" } else { $wrapperClass += "inline-block min-w-[120px]" }
        
        $out = "<div class="$wrapperClass">
"
        if ($id -ne "") {
            $out += "  <input type="hidden" id="$id" class="custom-select-input $class" value="$selectedValue">
"
        } else {
            $out += "  <input type="hidden" class="custom-select-input $class" value="$selectedValue">
"
        }
        $out += "  <div class="custom-select-trigger flex justify-between items-center w-full px-3 py-2 border border-[#333333] bg-[#1e1e1e] rounded-lg text-[#e0e0e0] cursor-pointer">
"
        $out += "    <span class="selected-text">$selectedText</span>
"
        $out += "    <svg class="h-4 w-4 text-gray-500 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
"
        $out += "  </div>
"
        $out += "  <ul class="custom-options absolute hidden z-50 w-full mt-1 bg-[#1e1e1e] border border-[#333333] rounded-lg shadow-lg overflow-y-auto max-h-60">
"
        $out += $lis
        $out += "  </ul>
"
        $out += "</div>"
        
        return $out
    })
    
    # Remove old trailing SVGs that were next to native selects in index.html
    $html = [regex]::Replace($html, '(?is)<div class="pointer-events-none absolute[^>]+>\s*<svg[^>]+>\s*<path[^>]+>\s*</svg>\s*</div>', '')
    
    [System.IO.File]::WriteAllText($file, $html, $utf8NoBom)
}
Write-Host "Replaced all select tags in HTML files."
