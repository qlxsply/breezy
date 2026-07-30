package com.corwin.framework.web.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Short-lived ticket service for SSE (Server-Sent Events) connections.
 *
 * <p>Design background:</p>
 * <ul>
 *   <li>SSE clients typically use the browser-native {@code EventSource} API.</li>
 *   <li>{@code EventSource} has limited support for custom request headers,
 *       making it impractical to carry a standard {@code Authorization: Bearer xxx}
 *       header in many real-world scenarios.</li>
 *   <li>A temporary credential mechanism dedicated to the SSE connection phase
 *       is needed, converting the regular login state into a short-lived ticket
 *       that can be passed via URL parameters.</li>
 * </ul>
 *
 * <p>Typical flow:</p>
 * <ol>
 *   <li>The frontend calls a ticket-issuing endpoint through a normal
 *       protected API (carrying JWT / Token).</li>
 *   <li>The server issues a short-lived, one-time SSE ticket based on the
 *       authenticated user's identity.</li>
 *   <li>The frontend accesses the SSE stream endpoint using the ticket
 *       as a query parameter.</li>
 *   <li>The authentication filter consumes the ticket and restores the
 *       user's identity context.</li>
 * </ol>
 *
 * <p>Design goals:</p>
 * <ul>
 *   <li>SSE connection only — does not replace the regular Token mechanism.</li>
 *   <li>The ticket must be short-lived.</li>
 *   <li>The ticket must be one-time consumable to prevent replay.</li>
 *   <li>Successful ticket authentication must recover the full
 *       {@link TokenPayload} for downstream context.</li>
 * </ul>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Tickets issued by this service must not be used as long-term credentials.</li>
 *   <li>Tickets are intended for brief transmission via HTTPS query parameters.</li>
 *   <li>Since query parameters may appear in access logs, keep the TTL short
 *       and avoid logging the ticket value in plain text.</li>
 * </ul>
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li>This interface does not mandate a specific storage backend;
 *       in-memory, Redis, or database implementations are all valid.</li>
 *   <li>An in-memory implementation suffices for single-node deployments;
 *       distributed deployments should use shared storage.</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/19
 */
public interface SseTicketService {

    /**
     * Issues a new short-lived SSE ticket.
     *
     * <p>This method is typically called after the user has been authenticated
     * via the normal Token mechanism, converting the current user identity
     * into a temporary ticket for SSE connection only.</p>
     *
     * <p>The returned ticket must satisfy:</p>
     * <ul>
     *   <li>Sufficiently random and hard to guess</li>
     *   <li>Has a well-defined expiration time</li>
     *   <li>Can be used later in {@link #consume(String)} to recover the user identity</li>
     * </ul>
     *
     * @param payload the current user identity payload, typically from a verified login session
     * @return the newly issued ticket, containing the ticket value and expiration time
     */
    SseTicket issue(TokenPayload payload);

    /**
     * Consumes an SSE ticket and recovers the original identity payload.
     *
     * <p>"Consume" implies the ticket is typically one-time: once successfully
     * read, it must not be reusable.</p>
     *
     * <p>Typical validation checks include:</p>
     * <ul>
     *   <li>Ticket is non-null</li>
     *   <li>Ticket exists in the store</li>
     *   <li>Ticket has not expired</li>
     *   <li>Ticket has not been used before</li>
     * </ul>
     *
     * <p>This method is typically called in the authentication filter,
     * effective only for SSE stream requests.</p>
     *
     * @param ticket the ticket value submitted by the frontend
     * @return the user identity payload bound to this ticket
     * @throws IllegalArgumentException if the ticket is null, invalid, expired, or already consumed
     */
    TokenPayload consume(String ticket);

    /**
     * Determines whether the current request should use SSE ticket authentication.
     *
     * <p>The purpose of this method is to restrict ticket authentication to
     * specific request paths, preventing normal APIs from inadvertently
     * accepting SSE tickets.</p>
     *
     * <p>Typical criteria:</p>
     * <ul>
     *   <li>HTTP method (typically GET)</li>
     *   <li>Request path matches the SSE stream endpoint</li>
     *   <li>Optionally, Content-Type or Accept headers for further narrowing</li>
     * </ul>
     *
     * @param request the current HTTP request
     * @return true if the request supports SSE ticket authentication; false otherwise
     */
    boolean supports(HttpServletRequest request);

    /**
     * Short-lived SSE ticket object.
     *
     * @param value                the ticket value, typically a high-entropy, URL-safe short string
     * @param expiresAtEpochMillis the ticket expiration timestamp in epoch millis
     */
    record SseTicket(
            String value,
            long expiresAtEpochMillis
    ) {
    }

}
