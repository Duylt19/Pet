/**
 * IPTV Channel Verifier Script
 * 
 * Fetches data from IPTV-org API, verifies stream URLs are accessible,
 * and outputs a curated JSON file with working channels.
 * 
 * Usage: node verify_channels.js
 * Output: channels_curated.json
 */

const https = require('https');
const http = require('http');
const fs = require('fs');
const url = require('url');

const BASE_URL = 'https://iptv-org.github.io/api/';
const MAX_CHANNELS_PER_CATEGORY = 50;
const CONCURRENCY = 25;
const STREAM_TIMEOUT = 8000; // 8 seconds

// --- Fetch helpers ---
function fetchJson(endpoint) {
    return new Promise((resolve, reject) => {
        const fullUrl = BASE_URL + endpoint;
        console.log(`  Fetching ${fullUrl}...`);
        https.get(fullUrl, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(new Error(`Parse error for ${endpoint}: ${e.message}`));
                }
            });
            res.on('error', reject);
        }).on('error', reject);
    });
}

/**
 * Check if a stream URL is accessible.
 * Returns true if the server responds with a success/redirect status.
 */
function checkStream(streamUrl) {
    return new Promise((resolve) => {
        try {
            const parsed = new URL(streamUrl);
            const client = parsed.protocol === 'https:' ? https : http;
            
            const req = client.get(streamUrl, {
                timeout: STREAM_TIMEOUT,
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                }
            }, (res) => {
                // Accept 200, 301, 302, 206 as "working"
                const ok = res.statusCode >= 200 && res.statusCode < 400;
                res.destroy(); // Don't download the stream
                resolve(ok);
            });
            
            req.on('error', () => resolve(false));
            req.on('timeout', () => {
                req.destroy();
                resolve(false);
            });
            
            // Safety: destroy after timeout
            setTimeout(() => {
                req.destroy();
            }, STREAM_TIMEOUT + 1000);
        } catch (e) {
            resolve(false);
        }
    });
}

/**
 * Run tasks with limited concurrency.
 */
async function parallelLimit(tasks, limit) {
    const results = [];
    let index = 0;
    
    async function worker() {
        while (index < tasks.length) {
            const i = index++;
            results[i] = await tasks[i]();
        }
    }
    
    const workers = Array.from({ length: Math.min(limit, tasks.length) }, () => worker());
    await Promise.all(workers);
    return results;
}

async function main() {
    console.log('=== IPTV Channel Verifier ===\n');
    
    // Step 1: Fetch all data
    console.log('[1/4] Fetching API data...');
    const [categories, channels, streams, logos] = await Promise.all([
        fetchJson('categories.json'),
        fetchJson('channels.json'),
        fetchJson('streams.json'),
        fetchJson('logos.json')
    ]);
    
    console.log(`  Categories: ${categories.length}`);
    console.log(`  Channels: ${channels.length}`);
    console.log(`  Streams: ${streams.length}`);
    console.log(`  Logos: ${logos.length}\n`);
    
    // Step 2: Build lookup maps
    console.log('[2/4] Building data maps...');
    
    // Stream URL by channel ID (first stream per channel)
    const streamMap = {};
    for (const s of streams) {
        if (!streamMap[s.channel]) {
            streamMap[s.channel] = s;
        }
    }
    
    // Logo by channel ID (first logo per channel)  
    const logoMap = {};
    for (const l of logos) {
        if (!logoMap[l.channel]) {
            logoMap[l.channel] = l.url;
        }
    }
    
    // Filter: non-NSFW channels that have streams
    const validChannels = channels.filter(ch => 
        !ch.is_nsfw && 
        !ch.categories.includes('xxx') &&
        streamMap[ch.id]
    );
    
    console.log(`  Channels with streams (non-NSFW): ${validChannels.length}\n`);
    
    // Step 3: Group by category and verify streams
    console.log('[3/4] Verifying streams (this may take a few minutes)...\n');
    
    const filteredCategories = categories.filter(c => c.id !== 'xxx');
    const curatedChannels = [];
    const usedChannelIds = new Set();
    
    for (const category of filteredCategories) {
        const categoryChannels = validChannels.filter(ch => 
            ch.categories.includes(category.id) && !usedChannelIds.has(ch.id)
        );
        
        if (categoryChannels.length === 0) {
            console.log(`  [${category.name}] No candidate channels, skipping.`);
            continue;
        }
        
        console.log(`  [${category.name}] Checking ${categoryChannels.length} candidates...`);
        
        const verified = [];
        let checked = 0;
        
        // Create verification tasks
        const tasks = categoryChannels.map(ch => async () => {
            const stream = streamMap[ch.id];
            const ok = await checkStream(stream.url);
            checked++;
            if (ok) {
                verified.push({
                    id: ch.id,
                    name: ch.name,
                    country: ch.country || '',
                    categories: ch.categories.filter(c => c !== 'xxx'),
                    logoUrl: logoMap[ch.id] || null,
                    streamUrl: stream.url,
                    quality: stream.quality || null
                });
            }
            // Stop early if we have enough
            if (verified.length >= MAX_CHANNELS_PER_CATEGORY) {
                return; // Won't check remaining
            }
        });
        
        // Run with concurrency limit, but stop when we have enough
        await parallelLimit(tasks, CONCURRENCY);
        
        // Take top N
        const selected = verified.slice(0, MAX_CHANNELS_PER_CATEGORY);
        for (const ch of selected) {
            usedChannelIds.add(ch.id);
            curatedChannels.push(ch);
        }
        
        console.log(`    ✓ ${selected.length} working / ${checked} checked`);
    }
    
    // Step 4: Output
    console.log(`\n[4/4] Writing curated JSON...`);
    
    const output = {
        version: 1,
        generatedAt: new Date().toISOString(),
        categories: filteredCategories.map(c => ({ id: c.id, name: c.name })),
        channels: curatedChannels
    };
    
    const jsonStr = JSON.stringify(output, null, 2);
    fs.writeFileSync('channels_curated.json', jsonStr, 'utf8');
    
    // Stats
    const stats = {};
    for (const ch of curatedChannels) {
        for (const cat of ch.categories) {
            stats[cat] = (stats[cat] || 0) + 1;
        }
    }
    
    console.log(`\n=== DONE ===`);
    console.log(`Total curated channels: ${curatedChannels.length}`);
    console.log(`File: channels_curated.json (${(jsonStr.length / 1024).toFixed(1)} KB)`);
    console.log(`\nPer category:`);
    for (const [cat, count] of Object.entries(stats).sort((a, b) => b[1] - a[1])) {
        console.log(`  ${cat}: ${count}`);
    }
}

main().catch(err => {
    console.error('FATAL:', err);
    process.exit(1);
});
