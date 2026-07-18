import re

def process_file():
    with open('doaa.txt', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    kotlin_code = []
    kotlin_code.append('    private fun seedPropheticAzkar(db: SQLiteDatabase) {')
    kotlin_code.append('        val category = "أدعية نبوية"')
    kotlin_code.append('        val propheticAzkar = listOf(')
    
    items = []
    for line in lines:
        line = line.strip()
        if not line:
            continue
        # some lines don't start with bullet but are prayers
        if line.startswith('•'):
            line = line[1:].strip()
        elif 'قال ﷺ' in line or 'وقال ﷺ' in line:
            # skipping introductory text if it's not a prayer
            if 'ادعوا الله' in line or 'إن ربكم' in line:
                continue
        
        # remove quotes
        line = line.replace('“', '').replace('”', '').replace('"', '')
        line = line.replace('{', '').replace('}', '')
        
        # separate text and virtue (e.g. رواه الترمذي, أخرجه مسلم)
        virtue = ""
        # split by last period
        parts = line.split('.')
        if len(parts) > 1:
            last_part = parts[-1].strip()
            second_last = parts[-2].strip()
            
            # Check if last or second last contains narrator
            narrators = ['رواه', 'أخرجه', 'متفق عليه']
            if any(n in last_part for n in narrators):
                virtue = last_part
                text = '.'.join(parts[:-1]).strip()
            elif any(n in second_last for n in narrators) and not last_part:
                virtue = second_last
                text = '.'.join(parts[:-2]).strip()
            else:
                text = line
        else:
            text = line
            
        # Clean text
        text = text.strip()
        virtue = virtue.strip()
        
        if text:
            # Escape for Kotlin string
            text_escaped = text.replace('"', '\\"')
            virtue_escaped = virtue.replace('"', '\\"')
            items.append(f'            AzkarItem(0, category, "دعاء", "{text_escaped}", "{virtue_escaped}", 1, 1, false)')
            
    kotlin_code.append(',\n'.join(items))
    kotlin_code.append('        )')
    kotlin_code.append('        for (item in propheticAzkar) {')
    kotlin_code.append('            val values = ContentValues().apply {')
    kotlin_code.append('                put(COL_CATEGORY, item.category)')
    kotlin_code.append('                put(COL_TITLE, item.title)')
    kotlin_code.append('                put(COL_TEXT, item.text)')
    kotlin_code.append('                put(COL_VIRTUES, item.virtues)')
    kotlin_code.append('                put(COL_COUNT, item.targetCount)')
    kotlin_code.append('                put(COL_IS_CUSTOM, if (item.isCustom) 1 else 0)')
    kotlin_code.append('            }')
    kotlin_code.append('            db.insert(TABLE_AZKAR, null, values)')
    kotlin_code.append('        }')
    kotlin_code.append('    }')
    
    with open('prophetic_azkar.kt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(kotlin_code))

if __name__ == '__main__':
    process_file()
