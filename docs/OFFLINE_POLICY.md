# Offline policy

Camera capture, image editing, local library, image-to-PDF, merge and split must work with Wi-Fi and mobile data disabled. Do not declare INTERNET permission in core V1. Ads and Play Billing are deferred and may require connectivity, but must never gate core functions.

No account, cloud OCR, document uploads or document content in logs/analytics. Test every P0 workflow in airplane mode and inspect the merged manifest.