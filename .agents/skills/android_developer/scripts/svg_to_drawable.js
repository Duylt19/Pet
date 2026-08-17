#!/usr/bin/env node

/**
 * Figma SVG to Android VectorDrawable Converter
 * 
 * Handles Figma-specific SVG quirks:
 * - CSS var() color references → fallback color
 * - Percentage width/height → use viewBox
 * - preserveAspectRatio="none" → remove
 * 
 * Usage:
 *   node svg_to_drawable.js <svg_url> <output_name> [--size 24] [--tint "#FFF"]
 * 
 * Examples:
 *   node svg_to_drawable.js http://localhost:3845/assets/xxx.svg tab_channel
 *   node svg_to_drawable.js http://localhost:3845/assets/xxx.svg search --size 24
 *   node svg_to_drawable.js http://localhost:3845/assets/xxx.svg heart --tint "#FF4444"
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');

// Paths
const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const DRAWABLE_DIR = path.join(PROJECT_ROOT, 'app', 'src', 'main', 'res', 'drawable');
const TEMP_DIR = path.join(PROJECT_ROOT, 'tools', 'temp_svg');

// Parse arguments
const args = process.argv.slice(2);
if (args.length < 2) {
    console.log(`
Usage: node svg_to_drawable.js <svg_url_or_file> <output_name> [options]

Options:
  --size <dp>     Set width/height in dp (default: 24)
  --tint <color>  Apply tint color (e.g. "#FFFFFF")

Examples:
  node svg_to_drawable.js http://localhost:3845/assets/xxx.svg tab_channel
  node svg_to_drawable.js http://localhost:3845/assets/xxx.svg search --size 24
  node svg_to_drawable.js ./icon.svg heart --tint "#FF4444"
`);
    process.exit(1);
}

const input = args[0];
const outputName = args[1];
let sizeDp = 24;
let tintColor = '';

// Parse optional flags
for (let i = 2; i < args.length; i++) {
    if (args[i] === '--size' && args[i + 1]) {
        sizeDp = parseInt(args[i + 1]);
        i++;
    } else if (args[i] === '--tint' && args[i + 1]) {
        tintColor = args[i + 1];
        i++;
    }
}

function fetchUrl(url) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        client.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => resolve(data));
        }).on('error', reject);
    });
}

/**
 * Pre-process Figma SVG to make it compatible with s2v converter.
 * Figma MCP exports SVGs with CSS var() references and percentage dimensions
 * that the converter doesn't understand.
 */
function cleanFigmaSvg(svgContent) {
    // Named CSS colors → hex (Figma sometimes uses color names in var() fallbacks)
    const namedColors = {
        'white': '#FFFFFF', 'black': '#000000', 'red': '#FF0000',
        'green': '#00FF00', 'blue': '#0000FF', 'yellow': '#FFFF00',
        'gray': '#808080', 'grey': '#808080', 'transparent': '#00000000',
        'none': 'none'
    };

    // 1. Replace CSS var() color references with their fallback values
    // e.g. fill="var(--fill-0, #FB2C36)" → fill="#FB2C36"
    // e.g. stroke="var(--stroke-0, white)" → stroke="white" → stroke="#FFFFFF"
    svgContent = svgContent.replace(/fill="var\(--fill-\d+,\s*([^)]+)\)"/g, 'fill="$1"');
    svgContent = svgContent.replace(/stroke="var\(--stroke-\d+,\s*([^)]+)\)"/g, 'stroke="$1"');
    
    // 2. Convert named colors to hex (for SVG attributes)
    for (const [name, hex] of Object.entries(namedColors)) {
        const regex = new RegExp(`(fill|stroke)="${name}"`, 'gi');
        svgContent = svgContent.replace(regex, `$1="${hex}"`);
    }
    
    // 3. Remove Figma-specific attributes that confuse the converter
    svgContent = svgContent.replace(/\s*preserveAspectRatio="none"/g, '');
    svgContent = svgContent.replace(/\s*overflow="visible"/g, '');
    svgContent = svgContent.replace(/\s*style="display:\s*block;"/g, '');
    
    // 4. Fix percentage width/height → remove them (viewBox will be used)
    svgContent = svgContent.replace(/\s*width="100%"/g, '');
    svgContent = svgContent.replace(/\s*height="100%"/g, '');
    
    return svgContent;
}


/**
 * Post-process the generated VectorDrawable XML:
 * - Fix width/height dp values
 * - Add tint if requested
 */
function postProcessXml(xmlContent) {
    // Fix the size: s2v sometimes outputs width/height based on viewBox
    // We want consistent dp values
    xmlContent = xmlContent.replace(
        /android:width="\d+(\.\d+)?dp"/,
        `android:width="${sizeDp}dp"`
    );
    xmlContent = xmlContent.replace(
        /android:height="\d+(\.\d+)?dp"/,
        `android:height="${sizeDp}dp"`
    );
    
    // Add tint if specified
    if (tintColor) {
        xmlContent = xmlContent.replace(
            '<vector',
            `<vector\n    android:tint="${tintColor}"`
        );
    }
    
    return xmlContent;
}

async function main() {
    console.log('🎨 Figma Icon Export');
    console.log(`   Input: ${input}`);
    console.log(`   Output: ic_${outputName}.xml (${sizeDp}dp)`);
    if (tintColor) console.log(`   Tint: ${tintColor}`);
    console.log('');
    
    // Ensure directories exist
    if (!fs.existsSync(TEMP_DIR)) fs.mkdirSync(TEMP_DIR, { recursive: true });
    if (!fs.existsSync(DRAWABLE_DIR)) fs.mkdirSync(DRAWABLE_DIR, { recursive: true });
    
    // Step 1: Get SVG content
    let svgContent;
    if (input.startsWith('http://') || input.startsWith('https://')) {
        console.log('📥 Downloading SVG...');
        svgContent = await fetchUrl(input);
    } else {
        console.log('📂 Reading SVG file...');
        svgContent = fs.readFileSync(input, 'utf8');
    }
    
    // Step 2: Clean Figma quirks
    console.log('🔧 Cleaning Figma SVG...');
    svgContent = cleanFigmaSvg(svgContent);
    
    // Save cleaned SVG
    const tempSvg = path.join(TEMP_DIR, `${outputName}.svg`);
    fs.writeFileSync(tempSvg, svgContent, 'utf8');
    
    // Step 3: Convert via s2v CLI
    console.log('⚙️  Converting to VectorDrawable...');
    const tempXml = path.join(TEMP_DIR, `${outputName}.xml`);
    try {
        execSync(`s2v -i "${tempSvg}" -o "${tempXml}"`, { stdio: 'pipe' });
    } catch (err) {
        console.error('❌ s2v conversion failed:', err.stderr?.toString() || err.message);
        process.exit(1);
    }
    
    // Step 4: Post-process
    console.log('✨ Post-processing...');
    let xmlContent = fs.readFileSync(tempXml, 'utf8');
    xmlContent = postProcessXml(xmlContent);
    
    // Step 5: Save to drawable
    const outputFile = path.join(DRAWABLE_DIR, `ic_${outputName}.xml`);
    fs.writeFileSync(outputFile, xmlContent, 'utf8');
    
    // Clean up temp files
    try { fs.unlinkSync(tempSvg); } catch(e) {}
    try { fs.unlinkSync(tempXml); } catch(e) {}
    
    // Show result
    console.log('');
    console.log('✅ SUCCESS!');
    console.log(`   File: ${outputFile}`);
    console.log('');
    console.log('--- Preview ---');
    console.log(xmlContent);
}

main().catch(err => {
    console.error('❌ Error:', err.message);
    process.exit(1);
});
