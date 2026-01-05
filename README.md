# Duebook Production Deployment Guide

This guide provides step-by-step instructions for deploying the Duebook application to a production server.

## Prerequisites

- Ubuntu/Debian-based server with sudo access
- Java installed (for running the Spring Boot application)
- Nginx installed
- Certbot for SSL certificates
- Database server (e.g., MySQL/PostgreSQL) configured

## Deployment Steps

1. **Navigate to the deployment directory:**
   ```bash
   cd /opt
   ```

2. **Create application directories:**
   ```bash
   mkdir app
   cd app
   mkdir duebook
   cd duebook
   mkdir config
   ```

3. **Copy configuration files:**
   - Copy your application configuration files (e.g., `application-prod.properties`) to the `/opt/app/duebook/config/` directory.

4. **Execute database scripts:**
   - Run the database schema scripts (e.g., `schema.sql`) to set up the database.

5. **Copy systemd service file:**
   - Copy the `duebook-app.service` file to `/etc/systemd/system/`.

6. **Enable and reload systemd:**
   ```bash
   sudo systemctl enable duebook-app
   sudo systemctl daemon-reload
   ```

7. **Copy Nginx configuration file:**
   - Copy the `duebook.famvest.online` file to `/etc/nginx/sites-available/`.

8. **Obtain SSL certificate:**
   ```bash
   sudo certbot --nginx -d duebook.famvest.online
   ```

9. **Enable Nginx site:**
   ```bash
   sudo cp /etc/nginx/sites-available/duebook.famvest.online /etc/nginx/sites-available/duebook.famvest.online
   cd /etc/nginx/sites-enabled
   sudo ln -sf /etc/nginx/sites-available/duebook.famvest.online duebook.famvest.online
   ```

10. **Restart services:**
    - Start the Duebook application: `sudo systemctl start duebook-app`
    - Restart Nginx: `sudo systemctl restart nginx`

## Notes

- Ensure all file paths and permissions are correctly set.
- Update configuration files with production-specific settings (database URLs, secrets, etc.).
- Monitor logs in `/opt/app/duebook/logs/` for any issues.
