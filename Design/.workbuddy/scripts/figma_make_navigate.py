#!/usr/bin/env python3
"""Capture the Figma Make preview iframe at full quality and navigate sections."""
import asyncio
import sys
from pathlib import Path
from playwright.async_api import async_playwright

URL = "https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes"
OUT_DIR = Path("/Users/shine/CommonWork/MobileWork/TideTunes/Design/.workbuddy/screenshots")


async def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True, args=["--disable-blink-features=AutomationControlled"])
        context = await browser.new_context(
            viewport={"width": 1920, "height": 1200},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            locale="en-US",
        )
        page = await context.new_page()
        try:
            await page.goto(URL, wait_until="domcontentloaded", timeout=60000)
        except Exception as e:
            print(f"[goto] {e}", file=sys.stderr)
        try:
            await page.wait_for_load_state("load", timeout=30000)
        except Exception as e:
            print(f"[load] {e}", file=sys.stderr)

        await page.wait_for_timeout(8000)

        # The DS preview is the figmaiframepreview.figma.site frame.
        preview_frame = None
        for f in page.frames:
            if "figmaiframepreview.figma.site" in (f.url or ""):
                preview_frame = f
                break

        if preview_frame is None:
            print("[error] preview frame not found", file=sys.stderr)
            await browser.close()
            return

        # Wait for the SPA inside the preview to render
        await page.wait_for_timeout(8000)

        # Resize the iframe container to a comfortable size
        try:
            await page.evaluate(
                """() => {
                    const iframes = document.querySelectorAll('iframe');
                    for (const f of iframes) {
                        if (f.src.includes('figmaiframepreview')) {
                            f.style.width = '1920px';
                            f.style.height = '1200px';
                            f.style.minHeight = '1200px';
                            f.removeAttribute('width');
                            f.removeAttribute('height');
                        }
                    }
                }"""
            )
            await page.wait_for_timeout(2000)
        except Exception as e:
            print(f"[resize] {e}", file=sys.stderr)

        # Page-level screenshot of the Figma Make outer UI
        out_outer = OUT_DIR / "figma_make_outer.png"
        await page.screenshot(path=str(out_outer), full_page=False)
        print(f"[ok] saved {out_outer}", file=sys.stderr)

        # Click each DS nav section inside the preview iframe
        for section in ["Home", "Search", "Library", "Settings", "Foundation", "Components", "Patterns"]:
            try:
                handle = await preview_frame.query_selector(f'text="{section}"')
                if handle:
                    await handle.click()
                    await page.wait_for_timeout(2500)
                    out = OUT_DIR / f"figma_make_section_{section.lower()}.png"
                    # Use page screenshot but only the iframe region
                    await page.screenshot(path=str(out), full_page=False)
                    print(f"[ok] saved {out} (clicked {section})", file=sys.stderr)
                else:
                    print(f"[warn] no element for {section}", file=sys.stderr)
            except Exception as e:
                print(f"[{section}] {e}", file=sys.stderr)

        # Try mobile preview toggle
        try:
            handle = await page.query_selector('text="Mobile preview"')
            if handle:
                await handle.click()
                await page.wait_for_timeout(2500)
                out = OUT_DIR / "figma_make_section_mobile.png"
                await page.screenshot(path=str(out), full_page=False)
                print(f"[ok] saved {out} (mobile)", file=sys.stderr)
        except Exception as e:
            print(f"[mobile] {e}", file=sys.stderr)

        # Also extract preview frame's full HTML
        try:
            html = await preview_frame.content()
            (OUT_DIR / "figma_make_preview.html").write_text(html, encoding="utf-8")
            print(f"[ok] saved preview.html ({len(html)} chars)", file=sys.stderr)
        except Exception as e:
            print(f"[preview html] {e}", file=sys.stderr)

        await browser.close()


if __name__ == "__main__":
    asyncio.run(main())
