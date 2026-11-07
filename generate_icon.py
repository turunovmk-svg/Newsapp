#!/usr/bin/env python3
"""Генерация иконки NewsApp в стиле OBSIDIAN / ELECTRIC BLUE.

Создаёт:
1. Адаптивную иконку (foreground + background + XML для mipmap-anydpi-v26)
2. Legacy PNG иконки для всех плотностей (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
"""

import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

# -------------------- Цветовая палитра --------------------
GRAPHITE_DEEP = (10, 10, 15, 255)       # #0A0A0F — глубокий графит
GRAPHITE_DARK = (18, 18, 30, 255)       # #12121E — чуть светлее
ELECTRIC_BLUE = (0, 242, 255, 255)      # #00F2FF — неоново-синий
ELECTRIC_BLUE_DIM = (0, 180, 200, 255)  # приглушённый синий
WHITE = (255, 255, 255, 255)
BLACK_OPAQUE = (0, 0, 0, 0)             # прозрачный фон для foreground

# -------------------- Размеры --------------------
# Legacy mipmap размеры
LEGACY_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}

# Адаптивная иконка: размер 108x108 dp (baseline), безопасная зона 66%
ADAPTIVE_SIZE = 1024  # рабочий холст для foreground/background
SAFE_ZONE_RATIO = 0.66


def create_background(size=ADAPTIVE_SIZE):
    """Слой фона адаптивной иконки: глубокий графит с синим акцентом снизу."""
    img = Image.new('RGBA', (size, size), GRAPHITE_DEEP)
    draw = ImageDraw.Draw(img)

    # Небольшой gradient-акцент снизу (электрический синий, мягкий)
    for y in range(int(size * 0.6), size):
        t = (y - int(size * 0.6)) / (size * 0.4)
        alpha = int(30 * t)
        draw.line([(0, y), (size, y)], fill=(0, 242, 255, alpha))

    # Эффект "стекло": мягкая светлая полоска в верхней части
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([size * 0.2, -size * 0.2, size * 0.8, size * 0.2],
                  fill=(0, 242, 255, 15))
    img = Image.alpha_composite(img, glow)

    # Тёмная vignette по краям для глубины
    vignette = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette)
    vdraw.ellipse([-size * 0.1, -size * 0.1, size * 1.1, size * 1.1],
                  fill=(0, 0, 0, 90))
    img = Image.alpha_composite(img, vignette)

    return img


def create_foreground(size=ADAPTIVE_SIZE):
    """Слой переднего плана: буква N с эффектом стекла и металла."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    safe = int(size * SAFE_ZONE_RATIO)
    margin = (size - safe) // 2

    # Рисуем букву N из трёх thick strokes
    stroke_w = int(size * 0.10)
    n_x1 = margin + int(size * 0.15)
    n_x2 = margin + int(size * 0.30)
    n_x3 = margin + int(size * 0.45)
    n_y_top = margin + int(size * 0.12)
    n_y_bot = margin + int(size * 0.88)

    # Левая вертикальная линия N
    draw.line([n_x1, n_y_top, n_x1, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)
    # Диагональ N
    draw.line([n_x1, n_y_top, n_x3, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)
    # Правая вертикальная линия N
    draw.line([n_x3, n_y_top, n_x3, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)

    # Добавляем "стеклянный" эффект: более светлые блики по краям штрихов
    glow_layer = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_layer)
    # Блик по левому краю штрихов
    gdraw.line([n_x1 - stroke_w // 3, n_y_top, n_x1 - stroke_w // 3, n_y_bot],
               fill=(255, 255, 255, 40), width=stroke_w // 2)
    gdraw.line([n_x3 - stroke_w // 3, n_y_top, n_x3 - stroke_w // 3, n_y_bot],
               fill=(255, 255, 255, 40), width=stroke_w // 2)
    img = Image.alpha_composite(img, glow_layer)

    # Мягкое внешнее свечение (блюм) для электрического синего
    blurred = img.filter(ImageFilter.GaussianBlur(radius=size * 0.02))
    img = Image.alpha_composite(blurred, img)

    return img


def round_corners(img, radius_ratio=0.18):
    """Закругляет углы изображения (адаптивная иконкаforeground/background могут быть обрезаны лаунчером)."""
    size = img.size[0]
    radius = int(size * radius_ratio)
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size, size], radius=radius, fill=255)
    img.putalpha(mask)
    return img


def create_adaptive_xmls(res_dir):
    """Создаёт XML-файлы адаптивной иконки в mipmap-anydpi-v26."""
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)

    # ic_launcher.xml — ссылка на background + foreground
    launcher_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''
    with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
        f.write(launcher_xml)

    # ic_launcher_round.xml — тоже adaptively (фон + передний план, round mask применяется лаунчером)
    round_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''
    with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
        f.write(round_xml)

    print(f"[OK] Adaptive XMLs created in {anydpi_dir}")


def create_background_drawable_xml(res_dir):
    """XML drawable для фона адаптивной иконки (использует PNG из mipmap-anydpi-v26)."""
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    bg_xml = '''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@mipmap/ic_launcher_background"
    android:gravity="center" />
'''
    with open(os.path.join(anydpi_dir, 'ic_launcher_background.xml'), 'w') as f:
        f.write(bg_xml)
    print(f"[OK] Background drawable XML created")


def create_foreground_drawable_xml(res_dir):
    """XML drawable для переднего плана адаптивной иконки."""
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    fg_xml = '''<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@mipmap/ic_launcher_foreground"
    android:gravity="center" />
'''
    with open(os.path.join(anydpi_dir, 'ic_launcher_foreground.xml'), 'w') as f:
        f.write(fg_xml)
    print(f"[OK] Foreground drawable XML created")


def save_png(img, path):
    """Сохраняет PNG с оптимизацией."""
    img.save(path, 'PNG', optimize=True)


def main():
    res_dir = 'app/src/main/res'

    # 1. Адаптивная иконка — background PNG
    bg = create_background()
    bg = round_corners(bg, radius_ratio=0.0)  # background не обрезаем, лаунчер сделает маску
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)
    save_png(bg, os.path.join(anydpi_dir, 'ic_launcher_background.png'))
    print("[OK] Background PNG saved")

    # 2. Адаптивная иконка — foreground PNG (буква N)
    fg = create_foreground()
    # foreground не должен иметь закруглений — лаунчер применяет свою маску
    save_png(fg, os.path.join(anydpi_dir, 'ic_launcher_foreground.png'))
    print("[OK] Foreground PNG saved")

    # 3. XML файлы адаптивной иконки
    create_adaptive_xmls(res_dir)
    create_background_drawable_xml(res_dir)
    create_foreground_drawable_xml(res_dir)

    # 4. Legacy PNG иконки для всех плотностей
    for density, size in LEGACY_SIZES.items():
        density_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(density_dir, exist_ok=True)

        # Рендерим legacy иконку: круглый фон с буквой N
        legacy = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(legacy)

        # Круглый фон графитовый
        margin = int(size * 0.06)
        draw.ellipse([margin, margin, size - margin, size - margin],
                     fill=GRAPHITE_DEEP)

        # Электрически-синий ободок
        draw.ellipse([margin, margin, size - margin, size - margin],
                     outline=ELECTRIC_BLUE, width=max(1, size // 48))

        # Внутренний светлый градиент-блюм
        glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow)
        gdraw.ellipse([int(size * 0.2), int(size * 0.2), int(size * 0.8), int(size * 0.8)],
                      fill=(0, 242, 255, 20))
        legacy = Image.alpha_composite(legacy, glow)

        # Буква N в центре
        stroke_w = max(2, size // 12)
        n_x1 = int(size * 0.28)
        n_x2 = int(size * 0.50)
        n_x3 = int(size * 0.72)
        n_y_top = int(size * 0.22)
        n_y_bot = int(size * 0.78)

        draw.line([n_x1, n_y_top, n_x1, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)
        draw.line([n_x1, n_y_top, n_x3, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)
        draw.line([n_x3, n_y_top, n_x3, n_y_bot], fill=ELECTRIC_BLUE, width=stroke_w)

        # Блик на штрихах
        gdraw2 = ImageDraw.Draw(legacy)
        gdraw2.line([n_x1 - 1, n_y_top, n_x1 - 1, n_y_bot], fill=(255, 255, 255, 60), width=max(1, stroke_w // 2))

        # ic_launcher (квадратный с закруглёнными углами для старых лаунчеров)
        legacy_rounded = legacy.copy()
        mask = Image.new('L', (size, size), 0)
        mdraw = ImageDraw.Draw(mask)
        mdraw.rounded_rectangle([0, 0, size, size], radius=int(size * 0.25), fill=255)
        legacy_rounded.putalpha(mask)
        save_png(legacy_rounded, os.path.join(density_dir, 'ic_launcher.png'))

        # ic_launcher_round (полностью круглый)
        legacy_round = legacy.copy()
        mask2 = Image.new('L', (size, size), 0)
        mdraw2 = ImageDraw.Draw(mask2)
        mdraw2.ellipse([0, 0, size, size], fill=255)
        legacy_round.putalpha(mask2)
        save_png(legacy_round, os.path.join(density_dir, 'ic_launcher_round.png'))

        print(f"[OK] Legacy {density}: {size}x{size}")

    print("\n=== Все иконки созданы успешно ===")


if __name__ == '__main__':
    main()
