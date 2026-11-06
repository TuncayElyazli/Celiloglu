import re
with open(r'C:\Users\Tuncay\Downloads\celiloglu\celiloglu\src\main\resources\templates\index.html', 'r', encoding='utf-8') as f:
    html = f.read()

start_idx = html.find('id="productGrid"')
if start_idx == -1:
    start_idx = html.find("id='productGrid'")

grid_content = html[start_idx:]
cards = re.split(r'<div class="product-card', grid_content)
for i in range(1, len(cards)):
    card = '<div class="product-card' + cards[i]
    if i == len(cards) - 1:
        # For the last card, strip the trailing stuff after its closing div
        # Let's just find the divs up to the end of the file.
        pass
        
    divs = card.count('<div')
    enddivs = card.count('</div')
    
    brand_match = re.search(r'data-brand="(.*?)"', card)
    code_match = re.search(r'data-code="(.*?)"', card)
    brand = brand_match.group(1) if brand_match else 'Unknown'
    code = code_match.group(1) if code_match else 'Unknown'
    
    print(f'Card {i} (Code: {code}, Brand: {brand}) has {divs} divs and {enddivs} /divs')
