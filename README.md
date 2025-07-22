# Markmap for IntelliJ IDEA
Visualize your markdown documents as interactive mind maps directly in JetBrains IDEs.
## Features
- Interactive Mind Maps: Transform your markdown files into beautiful, interactive mind maps
- Theme Integration: Seamlessly adapts to IntelliJ IDEA's light and dark themes
- Real-time Updates: Mind map updates automatically as you type
- Interactive Navigation: Pan, zoom, and explore your mind maps
- Multiple View Modes: Switch between editor-only, preview-only, or split view
- Zero Configuration: Works out of the box with any markdown file
- Coexistence: Works alongside the default markdown editor without conflicts

## Installation
### From JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to File → Settings → Plugins
3. Search for "Markmap"
4. Click Install
5. Restart the IDE

### Manual Installation
1. Download the latest release from the releases page
2. Go to File → Settings → Plugins
3. Click the gear icon and select Install Plugin from Disk...
4. Select the downloaded zip file
5. Restart the IDE

## Usage
1. Open any .md file in your JetBrains IDE
2. Select the "Markmap" tab at the bottom of the editor (next to the default markdown editor)
3. Use the toolbar buttons in the top-right corner to switch between:
    - Editor only: Focus on writing
    - Split view: See both editor and mind map (default)
    - Preview only: Focus on the mind map

Note: There is no need to uninstall the default markdown plugin. The Markmap editor appears as an additional tab after the default editor. Both editors can coexist perfectly.
### Requirements
- JCEF Support: Ensure that your IDE supports JCEF (Java Chromium Embedded Framework)
- IntelliJ Platform 2023.3+: Compatible with all modern JetBrains IDEs

### Example
Create a markdown file with hierarchical content and watch it transform into an interactive mind map:
# My Project

~~~markdown
---
title: markmap
markmap:
  colorFreezeLevel: 2
---

## Links

- [Website](https://markmap.js.org/)
- [GitHub](https://github.com/gera2ld/markmap)

## Related Projects

- [coc-markmap](https://github.com/gera2ld/coc-markmap) for Neovim
- [markmap-vscode](https://marketplace.visualstudio.com/items?itemName=gera2ld.markmap-vscode) for VSCode
- [eaf-markmap](https://github.com/emacs-eaf/eaf-markmap) for Emacs

## Features

Note that if blocks and lists appear at the same level, the lists will be ignored.

### Lists

- **strong** ~~del~~ *italic* ==highlight==
- `inline code`
- [x] checkbox
- Katex: $x = {-b \pm \sqrt{b^2-4ac} \over 2a}$ <!-- markmap: fold -->
  - [More Katex Examples](#?d=gist:af76a4c245b302206b16aec503dbe07b:katex.md)
- Now we can wrap very very very very long text with the `maxWidth` option
- Ordered list
  1. item 1
  2. item 2

### Blocks

```js
console.log('hello, JavaScript')
```
~~~

## Technology
This plugin is powered by the excellent markmap library, the same technology used in:
- Markmap VSCode Extension: [https://github.com/markmap/markmap-vscode](https://github.com/markmap/markmap-vscode)
- Markmap CLI: [https://github.com/markmap/markmap](https://github.com/markmap/markmap)

### Libraries Used
- markmap-lib: Core markmap functionality
- markmap-view: Interactive mind map rendering
- D3.js: Data visualization framework

## Troubleshooting
### JCEF Not Available
If you encounter issues with the mind map not displaying:
1. Go to Help → Find Action → Search for "Registry"
2. Find ide.browser.jcef.enabled and ensure it's checked
3. Restart the IDE

### Mind Map Not Updating
- Ensure you're using the "Markmap" tab, not just the default markdown editor
- Check that your markdown follows proper heading hierarchy

## Contributing
We welcome contributions! Please feel free to submit issues and pull requests.
### Development Setup
1. Clone the repository
2. Open in IntelliJ IDEA
3. Run: ./gradlew runIde

### Building
```shell
./gradlew buildPlugin
```
The built plugin will be in build/distributions/.
## Changelog
### 1.0.0
- Initial release
- Real-time markdown to mind map conversion
- Theme integration (dark/light mode support)
- Multiple view modes (editor, preview, split)
- Interactive navigation (pan, zoom)
- Coexistence with default markdown editor

## Credits
- Gerald and the markmap team for creating the amazing markmap library
- Markmap VSCode Extension for inspiration
- JetBrains for the excellent IntelliJ Platform

## License
This project is licensed under the MIT License - see the LICENSE file for details.
**Your plugin is ready for publication!** 🚀
