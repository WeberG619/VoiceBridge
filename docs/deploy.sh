#!/bin/bash

# VoiceBridge Documentation Deployment Script
# Deploys the documentation website to various hosting platforms

set -e

echo "🚀 VoiceBridge Documentation Deployment"
echo "========================================"

# Check if docs directory exists
if [ ! -d "docs" ]; then
    echo "❌ Error: docs directory not found"
    exit 1
fi

# Create deployment directory
DEPLOY_DIR="docs/dist"
mkdir -p "$DEPLOY_DIR"

# Copy documentation files
echo "📁 Copying documentation files..."
cp docs/*.html "$DEPLOY_DIR/"
cp docs/*.css "$DEPLOY_DIR/" 2>/dev/null || true
cp docs/*.js "$DEPLOY_DIR/" 2>/dev/null || true

# Copy assets if they exist
if [ -d "docs/assets" ]; then
    cp -r docs/assets "$DEPLOY_DIR/"
fi

# Copy Play Store graphics for website
if [ -d "store/graphics/generated" ]; then
    mkdir -p "$DEPLOY_DIR/images"
    cp store/graphics/generated/*.png "$DEPLOY_DIR/images/" 2>/dev/null || true
fi

# Create robots.txt
cat > "$DEPLOY_DIR/robots.txt" << EOF
User-agent: *
Allow: /

Sitemap: https://voicebridge.app/sitemap.xml
EOF

# Create sitemap.xml
cat > "$DEPLOY_DIR/sitemap.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
        <loc>https://voicebridge.app/</loc>
        <lastmod>$(date +%Y-%m-%d)</lastmod>
        <changefreq>weekly</changefreq>
        <priority>1.0</priority>
    </url>
    <url>
        <loc>https://voicebridge.app/getting-started.html</loc>
        <lastmod>$(date +%Y-%m-%d)</lastmod>
        <changefreq>monthly</changefreq>
        <priority>0.8</priority>
    </url>
    <url>
        <loc>https://voicebridge.app/privacy-policy.html</loc>
        <lastmod>$(date +%Y-%m-%d)</lastmod>
        <changefreq>monthly</changefreq>
        <priority>0.6</priority>
    </url>
</urlset>
EOF

# Create .htaccess for Apache servers
cat > "$DEPLOY_DIR/.htaccess" << EOF
# VoiceBridge Website Configuration

# Enable compression
<IfModule mod_deflate.c>
    AddOutputFilterByType DEFLATE text/plain
    AddOutputFilterByType DEFLATE text/html
    AddOutputFilterByType DEFLATE text/xml
    AddOutputFilterByType DEFLATE text/css
    AddOutputFilterByType DEFLATE application/xml
    AddOutputFilterByType DEFLATE application/xhtml+xml
    AddOutputFilterByType DEFLATE application/rss+xml
    AddOutputFilterByType DEFLATE application/javascript
    AddOutputFilterByType DEFLATE application/x-javascript
</IfModule>

# Set cache headers
<IfModule mod_expires.c>
    ExpiresActive on
    ExpiresByType text/css "access plus 1 month"
    ExpiresByType application/javascript "access plus 1 month"
    ExpiresByType image/png "access plus 1 month"
    ExpiresByType image/jpg "access plus 1 month"
    ExpiresByType image/jpeg "access plus 1 month"
    ExpiresByType image/gif "access plus 1 month"
    ExpiresByType image/svg+xml "access plus 1 month"
</IfModule>

# Security headers
Header always set X-Content-Type-Options nosniff
Header always set X-Frame-Options DENY
Header always set X-XSS-Protection "1; mode=block"
Header always set Referrer-Policy "strict-origin-when-cross-origin"
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"

# Redirect to HTTPS
RewriteEngine On
RewriteCond %{HTTPS} off
RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
EOF

echo "✅ Documentation prepared for deployment"
echo ""
echo "📊 Deployment Summary:"
echo "======================"
echo "📁 Files copied to: $DEPLOY_DIR"
echo "📄 HTML files: $(find $DEPLOY_DIR -name "*.html" | wc -l)"
echo "🖼️  Image files: $(find $DEPLOY_DIR -name "*.png" -o -name "*.jpg" -o -name "*.gif" | wc -l)"
echo "📝 SEO files: robots.txt, sitemap.xml"
echo "⚙️  Server config: .htaccess"
echo ""

# Display deployment instructions
echo "🌐 Deployment Instructions:"
echo "============================"
echo ""
echo "1. GitHub Pages:"
echo "   git add docs/dist && git commit -m 'Deploy documentation'"
echo "   git push origin main"
echo "   Enable GitHub Pages in repository settings"
echo ""
echo "2. Netlify:"
echo "   cd $DEPLOY_DIR && netlify deploy --prod"
echo "   Or drag and drop the dist folder to Netlify dashboard"
echo ""
echo "3. Vercel:"
echo "   cd $DEPLOY_DIR && vercel --prod"
echo ""
echo "4. Traditional hosting:"
echo "   Upload contents of $DEPLOY_DIR to your web server"
echo "   Point domain to the uploaded directory"
echo ""
echo "5. Firebase Hosting:"
echo "   firebase init hosting"
echo "   firebase deploy"
echo ""

# Create GitHub Pages workflow
mkdir -p .github/workflows

cat > .github/workflows/deploy-docs.yml << EOF
name: Deploy Documentation

on:
  push:
    branches: [ main, master ]
    paths: [ 'docs/**' ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Pages
        uses: actions/configure-pages@v3
        
      - name: Deploy documentation
        run: |
          chmod +x docs/deploy.sh
          ./docs/deploy.sh
          
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v2
        with:
          path: docs/dist
          
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v2
EOF

echo "✅ GitHub Pages workflow created"
echo ""
echo "🎉 Documentation deployment ready!"
echo "   The VoiceBridge website is prepared for hosting on multiple platforms."