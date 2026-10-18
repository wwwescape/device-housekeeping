"""Generates the Device Housekeeping logo, launcher icons and TV banner.

Like Device Info, each device class gets its own launcher icon: a Material Symbols Outlined
device outline (FILL 0, wght 400, GRAD 0, opsz 24) with "delete_sweep" drawn inside its screen.

    phone  -> "mobile"          drawable/            (the default, and the brand logo)
    tablet -> "tablet_android"  drawable-sw600dp/
    tv     -> "tv"              drawable-television/
    watch  -> "watch"           drawable-watch/

The adaptive-icon XML is shared; Android picks the foreground by resource qualifier. The
status-bar icon is "delete_sweep" on its own, which reads better at 24dp.

Outputs follow the sibling apps' conventions: #2A7FFF glyph on white, a monochrome variant for
themed icons, a white status-bar icon, a 320x180dp Google TV banner, assets/logo.svg, and design/
PNGs for the Play Store.

Usage: python scripts/generate_icons.py   (needs resvg-py: pip install resvg-py)
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
BLUE = "#2A7FFF"
INK = "#1C1B1F"
BANNER_FONT = ROOT / "design" / "fonts" / "roboto_flex_medium.ttf"

# All glyphs use the Material Symbols viewBox: 0 -960 960 960.
# "mobile" without its camera dot (the last subpath), matching Device Info's phone outline.
MOBILE = (
    "M280-40q-33 0-56.5-23.5T200-120v-720q0-33 23.5-56.5T280-920h400q33 0 56.5 23.5T760-840v124"
    "q18 7 29 22t11 34v80q0 19-11 34t-29 22v404q0 33-23.5 56.5T680-40H280Zm0-80h400v-720H280v720Z"
    "m0 0v-720 720Z"
)
TABLET = (
    "M200-40q-33 0-56.5-23.5T120-120v-720q0-33 23.5-56.5T200-920h560q33 0 56.5 23.5T840-840v720"
    "q0 33-23.5 56.5T760-40H200Zm0-200v120h560v-120H200Zm200 80h160v-40H400v40ZM200-320h560v-400"
    "H200v400Zm0-480h560v-40H200v40Zm0 0v-40 40Zm0 560v120-120Z"
)
TV = (
    "M320-120v-80H160q-33 0-56.5-23.5T80-280v-480q0-33 23.5-56.5T160-840h640q33 0 56.5 23.5T880-760"
    "v480q0 33-23.5 56.5T800-200H640v80H320ZM160-280h640v-480H160v480Zm0 0v-480 480Z"
)
WATCH = (
    "M420-800h120-120Zm0 640h120-120Zm-60 80-54-182q-48-38-77-95t-29-123q0-66 29-123t77-95l54-182"
    "h240l54 182q48 38 77 95t29 123q0 66-29 123t-77 95L600-80H360Zm261.5-258.5Q680-397 680-480"
    "t-58.5-141.5Q563-680 480-680t-141.5 58.5Q280-563 280-480t58.5 141.5Q397-280 480-280"
    "t141.5-58.5ZM404-750q20-5 38.5-8t37.5-3q19 0 37.5 3t38.5 8l-16-50H420l-16 50Zm16 590h120l16-50"
    "q-20 5-38.5 7.5T480-200q-19 0-37.5-2.5T404-210l16 50Z"
)
# delete_sweep spans x 80..880, y -800..-200: 800 x 600, centered on (480, -500).
SWEEP = (
    "M600-240v-80h160v80H600Zm0-320v-80h280v80H600Zm0 160v-80h240v80H600ZM120-640H80v-80h160v-60"
    "h160v60h160v80h-40v360q0 33-23.5 56.5T440-200H200q-33 0-56.5-23.5T120-280v-360Zm80 0v360h240"
    "v-360H200Zm0 0v360-360Z"
)

# variant: (device outline, center of its screen, scale for SWEEP to fit that screen with margin)
VARIANTS = {
    "phone": (MOBILE, (480.0, -480.0), 0.45),   # screen 400 x 720
    "tablet": (TABLET, (480.0, -520.0), 0.55),  # screen 560 x 400
    "tv": (TV, (480.0, -520.0), 0.62),          # screen 640 x 480
    "watch": (WATCH, (480.0, -480.0), 0.37),    # round face, radius 200
}
QUALIFIERS = {"phone": "", "tablet": "-sw600dp", "tv": "-television", "watch": "-watch"}


def sweep_transform(variant):
    _, (cx, cy), s = VARIANTS[variant]
    return s, cx - 480 * s, cy + 500 * s


def vector(width_dp, viewport, body, height_dp=None, viewport_height=None):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width_dp}dp"\n'
        f'    android:height="{height_dp or width_dp}dp"\n'
        f'    android:viewportWidth="{viewport}"\n'
        f'    android:viewportHeight="{viewport_height or viewport}">\n'
        f"{body}"
        "</vector>\n"
    )


def path_xml(color, data, indent):
    return f'{indent}<path\n{indent}    android:fillColor="{color}"\n{indent}    android:pathData="{data}" />\n'


def glyph(variant, color, indent):
    """Device outline plus the scaled sweep, in the 960 viewBox coordinate space."""
    device = VARIANTS[variant][0]
    s, tx, ty = sweep_transform(variant)
    return (
        path_xml(color, device, indent)
        + f"{indent}<group\n"
        f'{indent}    android:scaleX="{s}"\n'
        f'{indent}    android:scaleY="{s}"\n'
        f'{indent}    android:translateX="{tx:.1f}"\n'
        f'{indent}    android:translateY="{ty:.1f}">\n'
        + path_xml(color, SWEEP, indent + "    ")
        + f"{indent}</group>\n"
    )


def group(variant, scale, tx, ty, color):
    return (
        "    <group\n"
        f'        android:scaleX="{scale}"\n'
        f'        android:scaleY="{scale}"\n'
        f'        android:translateX="{tx}"\n'
        f'        android:translateY="{ty}">\n'
        f"{glyph(variant, color, '        ')}"
        "    </group>\n"
    )


def svg_glyph(variant, color):
    device = VARIANTS[variant][0]
    s, tx, ty = sweep_transform(variant)
    return (
        f'<path fill="{color}" d="{device}"/>'
        f'<g transform="translate({tx:.1f} {ty:.1f}) scale({s})"><path fill="{color}" d="{SWEEP}"/></g>'
    )


def write(rel, text):
    target = RES / rel if not rel.startswith("/") else ROOT / rel[1:]
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8", newline="\n")


WHITE_1200 = '    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="M0,0h1200v1200h-1200z" />\n'


def main():
    for variant, q in QUALIFIERS.items():
        # Adaptive icon: 108dp canvas with the glyph inside the 66dp safe zone (same placement
        # as the sibling apps). The phone set lives in the unqualified folders.
        write(f"drawable{q}/ic_launcher_foreground.xml", vector(108, 1200, group(variant, 0.5625, 330, 870, BLUE)))
        write(f"drawable{q}/ic_launcher_monochrome.xml", vector(108, 1200, group(variant, 0.5625, 330, 870, "#FFFFFFFF")))
        # In-app logo, About icon and splash follow the device too. UI-mode and size qualifiers
        # outrank density, so these win over the drawable-nodpi defaults on tablets/TV/watch.
        nodpi = "drawable-nodpi" if not q else f"drawable{q}"
        write(f"{nodpi}/splash_icon.xml", vector(108, 1200, group(variant, 0.5625, 330, 870, BLUE)))
        write(f"{nodpi}/ic_app_icon.xml", vector(48, 1200, WHITE_1200 + group(variant, 0.8125, 210, 990, BLUE)))
        write(f"{nodpi}/ic_logo_mark.xml", vector(48, 960, '    <group android:translateY="960">\n' + glyph(variant, BLUE, "        ") + "    </group>\n"))

    write(
        "drawable/ic_launcher_background.xml",
        vector(108, 108, '    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="M0,0h108v108h-108z" />\n'),
    )
    write("drawable/ic_stat_clean.xml", vector(24, 960, '    <group android:translateY="960">\n' + path_xml("#FFFFFFFF", SWEEP, "        ") + "    </group>\n"))
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        "</adaptive-icon>\n"
    )
    write("mipmap-anydpi-v26/ic_launcher.xml", adaptive)
    write("mipmap-anydpi-v26/ic_launcher_round.xml", adaptive)

    logo = f'<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px">{svg_glyph("phone", BLUE)}</svg>\n'
    write("/assets/logo.svg", logo)

    import resvg_py

    def png(svg, out, **kwargs):
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(bytes(resvg_py.svg_to_bytes(svg_string=svg, **kwargs)))

    # Play Store icon: full-bleed white square, glyph at the same proportion as the launcher icon.
    png(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 1200 1200">'
        f'<rect width="1200" height="1200" fill="#fff"/>'
        f'<g transform="translate(210 990) scale(0.8125)">{svg_glyph("phone", BLUE)}</g></svg>',
        ROOT / "design" / "logo-playstore-512.png",
    )
    for variant in VARIANTS:
        png(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="960" height="960" viewBox="0 -960 960 960">'
            f"{svg_glyph(variant, BLUE)}</svg>",
            ROOT / "design" / ("logo-source.png" if variant == "phone" else f"logo-source-{variant}.png"),
        )

    # Google TV banner: 320x180dp, rendered at xhdpi (640x360). The TV glyph on the left, the
    # app name beside it.
    png(
        '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">'
        '<rect width="640" height="360" fill="#fff"/>'
        f'<g transform="translate(36 288) scale(0.225)">{svg_glyph("tv", BLUE)}</g>'
        f'<text x="276" y="166" font-family="Roboto Flex" font-size="52" font-weight="500" fill="{INK}">Device</text>'
        f'<text x="276" y="230" font-family="Roboto Flex" font-size="52" font-weight="500" fill="{INK}">Housekeeping</text>'
        "</svg>",
        RES / "drawable-xhdpi" / "tv_banner.png",
        skip_system_fonts=True,
        font_files=[str(BANNER_FONT)],
    )


if __name__ == "__main__":
    main()
