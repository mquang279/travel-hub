def main():
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        page.goto("https://example.com")

        # lấy HTML
        html = page.content()
        print(html)
        print(":D")
        # lấy data bằng selector
        titles = page.locator("h2.title").all_text_contents()
        print(titles)

        browser.close()

if __name__ == "__main__":
    main()
