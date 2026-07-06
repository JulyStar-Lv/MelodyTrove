#!/usr/bin/env python3
"""Debug: list all frames after page load."""
import asyncio
from pathlib import Path
from playwright.async_api import async_playwright

URL = "https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes"
OUT_DIR = Path("/Users/shine/CommonWork/MobileWork/TideTunes/Design/.workbuddy/screenshots")


async def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1920, "height": 1200},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )
        page = await context.new_page()
        await page.goto(URL, wait_until="domcontentloaded", timeout=60000)
        await page.wait_for_timeout(12000)

        # All frames
        frames = page.frames
        print(f"frames: {len(frames)}")
        for i, f in enumerate(frames):
            print(f"  [{i}] {f.url[:120]}")

        # All iframes on page
        iframes = await page.evaluate(
            """() => Array.from(document.querySelectorAll('iframe')).map(f => ({src: f.src, w: f.offsetWidth, h: f.offsetHeight, name: f.name}))"""
        )
        print(f"iframes in DOM: {len(iframes)}")
        for i, info in enumerate(iframes):
            print(f"  [{i}] {info}")

        # Look for the preview iframe name
        preview_info = await page.evaluate(
            """() => {
                const all = document.querySelectorAll('iframe');
                for (const f of all) {
                    if (f.src.includes('figmaiframepreview')) return {found: true, src: f.src, w: f.offsetWidth, h: f.offsetHeight};
                }
                return {found: false};
            }"""
        )
        print(f"preview: {preview_info}")

        # Save a screenshot of where we are
        await page.screenshot(path=str(OUT_DIR / "debug_outer.png"), full_page=False)
        print(f"saved debug_outer.png")

        await browser.close()


if __name__ == "__main__":
    asyncio.run(main())
