import os, glob

# Change br_border_ads_view.xml
f_path = r'ads\src\main\res\drawable\br_border_ads_view.xml'
if os.path.exists(f_path):
    with open(f_path, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('#F5F5F5', '#2a2a2a')
    c = c.replace('@color/white', '#222222')
    with open(f_path, 'w', encoding='utf-8') as f:
        f.write(c)

# Change bg_button_open_ads.xml
f_path = r'ads\src\main\res\drawable\bg_button_open_ads.xml'
if os.path.exists(f_path):
    with open(f_path, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('android:endColor="#0570FC"', 'android:angle="270"\n        android:endColor="#D11E25"')
    c = c.replace('android:startColor="#37AEFE"', 'android:startColor="#FB2C36"')
    with open(f_path, 'w', encoding='utf-8') as f:
        f.write(c)

# Change ic_label_ad_primary.xml
f_path = r'ads\src\main\res\drawable\ic_label_ad_primary.xml'
if os.path.exists(f_path):
    with open(f_path, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('android:color="#37AEFE"', 'android:color="#FB2C36"')
    c = c.replace('android:color="#0570FC"', 'android:color="#D11E25"')
    with open(f_path, 'w', encoding='utf-8') as f:
        f.write(c)

# Change layout_native_ad*.xml and holder_*.xml text colors
layout_files = glob.glob(r'ads\src\main\res\layout\*.xml')
for f_path in layout_files:
    if os.path.exists(f_path):
        with open(f_path, 'r', encoding='utf-8') as f:
            c = f.read()
        changed = False
        if '@color/black' in c:
            c = c.replace('@color/black', '@android:color/white')
            changed = True
        if '#FF333333' in c:
            c = c.replace('#FF333333', '@android:color/white')
            changed = True
        if '@color/text_color_99' in c:
            c = c.replace('@color/text_color_99', '@android:color/white')
            changed = True
        if changed:
            with open(f_path, 'w', encoding='utf-8') as f:
                f.write(c)
