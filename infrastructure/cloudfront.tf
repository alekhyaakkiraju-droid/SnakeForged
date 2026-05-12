# ── CloudFront origin access ──────────────────────────────────────────────────

locals {
  cf_origin_id = "${local.name_prefix}-alb-origin"
}

# ── Cache policies ────────────────────────────────────────────────────────────

# Static assets — long TTL; content is versioned by filename hash in production
resource "aws_cloudfront_cache_policy" "static_assets" {
  name        = "${local.name_prefix}-static-assets"
  comment     = "Long TTL for versioned static assets (JS, CSS, images)"
  default_ttl = 86400   # 1 day
  max_ttl     = 2592000 # 30 days
  min_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "none"
    }
    query_strings_config {
      query_string_behavior = "none"
    }
    enable_accept_encoding_gzip   = true
    enable_accept_encoding_brotli = true
  }
}

# API responses — short TTL or no cache; forward query strings and headers
resource "aws_cloudfront_cache_policy" "api" {
  name        = "${local.name_prefix}-api"
  comment     = "Short TTL for API responses"
  default_ttl = 0
  max_ttl     = 30
  min_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "whitelist"
      headers {
        items = ["Authorization", "Content-Type"]
      }
    }
    query_strings_config {
      query_string_behavior = "all"
    }
    enable_accept_encoding_gzip   = true
    enable_accept_encoding_brotli = true
  }
}

# ── Origin request policy ─────────────────────────────────────────────────────
# Forwards Host header so Spring Boot receives the correct domain name

resource "aws_cloudfront_origin_request_policy" "forward_host" {
  name    = "${local.name_prefix}-forward-host"
  comment = "Forward Host header to ALB origin"

  cookies_config {
    cookie_behavior = "none"
  }
  headers_config {
    header_behavior = "whitelist"
    headers {
      items = ["Host"]
    }
  }
  query_strings_config {
    query_string_behavior = "all"
  }
}

# ── CloudFront distribution ───────────────────────────────────────────────────

resource "aws_cloudfront_distribution" "main" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "SnakeForged ${var.environment}"
  aliases             = [var.domain_name]
  price_class         = "PriceClass_100" # US/EU/Canada only — cheapest tier
  http_version        = "http2and3"
  wait_for_deployment = false

  origin {
    domain_name = aws_lb.main.dns_name
    origin_id   = local.cf_origin_id

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # Default behaviour: serve static content (HTML, CSS, JS, images)
  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = local.cf_origin_id
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    cache_policy_id          = aws_cloudfront_cache_policy.static_assets.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.forward_host.id
  }

  # API paths — bypass static cache, forward all query strings
  ordered_cache_behavior {
    path_pattern           = "/api/*"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = local.cf_origin_id
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    cache_policy_id          = aws_cloudfront_cache_policy.api.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.forward_host.id
  }

  # Actuator health endpoints — never cache
  ordered_cache_behavior {
    path_pattern           = "/actuator/*"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = local.cf_origin_id
    viewer_protocol_policy = "redirect-to-https"
    compress               = false

    cache_policy_id          = aws_cloudfront_cache_policy.api.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.forward_host.id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.main.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }
}
