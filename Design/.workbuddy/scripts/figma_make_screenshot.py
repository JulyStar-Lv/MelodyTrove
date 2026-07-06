#!/usr/bin/env python3
"""Open Figma Make URL, wait for content to load, take full-page screenshot."""
import asyncio
import sys
from pathlib import Path
from playwright.async_api import async_playwright

URL = "https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes"
OUT_DIR = Path("/Users/shine/CommonWork/MobileWork/TideTunes/Design/.workbuddy/screenshots")


async def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1440, "height": 900},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )
        page = await context.new_page()
        try:
            await page.goto(URL, wait_until="domcontentloaded", timeout=60000)
        except Exception as e:
            print(f"[goto error] {e}", file=sys.stderr)

        # Wait for the SPA to render. Figma Make can take 5-15s to fully render.
        try:
            await page.wait_for_load_state("networkidle", timeout=30000)
        except Exception as e:
            print(f"[networkidle error] {e}", file=sys.stderr)
        try:
            await page.wait_for_load_state("load", timeout=15000)
        except Exception as e:
            print(f"[load error] {e}", file=sys.stderr)

        # Settle
        await page.wait_for_timeout(5000)

        # Save full-page screenshot
        out1 = OUT_DIR / "figma_make_full.png"
        await page.screenshot(path=str(out1), full_page=True)
        print(f"[ok] saved {out1}")

        # Save viewport screenshot
        out2 = OUT_DIR / "figma_make_viewport.png"
        await page.screenshot(path=str(out2), full_page=False)
        print(f"[ok] saved {out2}")

        # Try to extract any visible text content
        try:
            text = await page.evaluate(
                """() => {
                    const all = document.body.innerText;
                    return all.substring(0, 8000);
                }"""
            )
            out3 = OUT_DIR / "figma_make_text.txt"
            out3.write_text(text, encoding="utf-8")
            print(f"[ok] saved {out3} ({len(text)} chars)")
        except Exception as e:
            print(f"[extract text error] {e}", file=sys.stderr)

        # Try to find the canvas / iframe / main element
        try:
            html = await page.content()
            out4 = OUT_DIR / "figma_make.html"
            out4.write_text(html, encoding="utf-8")
            print(f"[ok] saved {out4} ({len(html)} chars)")
        except Exception as e:
            print(f"[content error] {e}", file=sys.stderr)

        await browser.close()


if __name__ == "__main__":
    asyncio.run(main())
