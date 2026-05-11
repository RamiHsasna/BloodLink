<?php

namespace App\Service;

/**
 * Service to generate beautiful, unified HTML email templates
 * based on the BloodLink premium design.
 */
class EmailTemplateService
{
    /**
     * Render a premium HTML email template.
     *
     * @param string $title       The main heading (e.g., "Demande urgente de sang")
     * @param string $contentHtml The main message body in HTML
     * @param string $buttonLabel The text for the CTA button
     * @param string $buttonUrl   The URL for the CTA button
     * @param string $footerText  Optional custom footer text
     *
     * @return string Complete HTML document
     */
    public function render(
        string $title,
        string $contentHtml,
        string $buttonLabel = 'Check ',
        string $buttonUrl = '#',
        string $footerText = 'Regards,'
    ): string {
        $primaryColor = '#c52228';
        $bgColor = '#f6f9fc';

        return <<<HTML
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { margin: 0; padding: 0; background-color: {$bgColor}; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333333; }
        .wrapper { width: 100%; table-layout: fixed; background-color: {$bgColor}; padding-bottom: 40px; padding-top: 40px; }
        .main { background-color: #ffffff; width: 100%; max-width: 600px; margin: 0 auto; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
        .header { padding: 30px 40px; text-align: left; }
        .logo-text { color: {$primaryColor}; font-size: 28px; font-weight: 700; display: inline-block; vertical-align: middle; }
        .logo-icon { display: inline-block; vertical-align: middle; margin-right: 10px; width: 32px; height: 32px; background-color: {$primaryColor}; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); margin-top: -5px; }
        .content { padding: 0 40px 40px 40px; }
        .title { font-size: 22px; font-weight: 700; margin: 0 0 24px 0; color: #1a1a1a; }
        .body-text { font-size: 16px; line-height: 1.6; color: #444444; margin-bottom: 30px; }
        .cta-container { text-align: center; margin-bottom: 40px; }
        .button { background-color: {$primaryColor}; color: #ffffff !important; padding: 16px 32px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 16px; display: inline-block; transition: background-color 0.2s; }
        .footer { padding: 0 40px 40px 40px; font-size: 14px; color: #666666; line-height: 1.5; }
        .footer-signoff { margin-top: 20px; font-weight: 600; color: #333333; }
        @media only screen and (max-width: 600px) {
            .header, .content, .footer { padding-left: 25px; padding-right: 25px; }
        }
    </style>
</head>
<body>
    <div class="wrapper">
        <div class="main">
            <!-- Header with Logo -->
            <div class="header">
                <div class="logo-icon"></div>
                <span class="logo-text">BloodLink</span>
            </div>

            <!-- Content Section -->
            <div class="content">
                <h1 class="title">{$title}</h1>
                <div class="body-text">
                    {$contentHtml}
                </div>

                <!-- Action Button -->
                <div class="cta-container">
                    <a href="{$buttonUrl}" class="button">{$buttonLabel}</a>
                </div>
            </div>

            <!-- Footer -->
            <div class="footer">
                <p>{$footerText}</p>
                <p class="footer-signoff">BloodLink Team</p>
            </div>
        </div>
    </div>
</body>
</html>
HTML;
    }
}
