import React, { useEffect, useState } from 'react'
import Admin from './Admin.jsx'

// === Keycloak config ===
const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8085'
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'shop'
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT || 'frontend'

const AUTH_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`
const TOKEN_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`
const LOGOUT_URL =
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`

// ===== PKCE helpers =====
function base64UrlEncode(buffer) {
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '')
}

async function createPkcePair() {
    // 32 random bytes → verifier
    const random = new Uint8Array(32)
    crypto.getRandomValues(random)

    const verifier = base64UrlEncode(random)

    // SHA-256(verifier) → challenge
    const encoder = new TextEncoder()
    const data = encoder.encode(verifier)
    const digest = await crypto.subtle.digest('SHA-256', data)
    const challenge = base64UrlEncode(digest)

    return { verifier, challenge }
}

export default function App() {
    const [token, setToken] = useState(null)
    const [authenticated, setAuthenticated] = useState(false)
    const [ready, setReady] = useState(false)
    const [tab, setTab] = useState('shop')
    const [products, setProducts] = useState([])

    // ===== API =====
    async function fetchProducts(tok) {
        try {
            const res = await fetch(
                (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
                '/store/api/products',
                {
                    headers: { Authorization: 'Bearer ' + tok },
                },
            )
            if (!res.ok) {
                console.error('Fetch products failed, status =', res.status)
                return
            }
            const data = await res.json()
            setProducts(data)
        } catch (e) {
            console.error('Fetch products error:', e)
        }
    }

    async function orderOne(id) {
        if (!token) return
        await fetch(
            (import.meta.env.VITE_API_BASE || 'http://localhost:8080') +
            `/order/api/orders`,
            {
                method: 'POST',
                headers: {
                    Authorization: 'Bearer ' + token,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    customerId: 1,
                    items: [{ productId: id, quantity: 1, price: 0 }],
                }),
            },
        )
        alert('Order created; will confirm asynchronously')
    }

    // ===== Ручная обработка возвращённого code из Keycloak =====
    useEffect(() => {
        const doAuthFlow = async () => {
            console.log('APP START URL:', window.location.href)

            const url = new URL(window.location.href)

            // 1) достаём code из query (?code=...)
            let code = url.searchParams.get('code')

            // 2) если в query нет — пробуем из hash (#code=...)
            if (!code) {
                const rawHash = window.location.hash.startsWith('#')
                    ? window.location.hash.substring(1)
                    : window.location.hash

                const hashParams = new URLSearchParams(rawHash)
                code = hashParams.get('code')
            }

            if (code) {
                console.log('Found authorization code in URL, exchanging for token...')

                const body = new URLSearchParams()
                body.set('grant_type', 'authorization_code')
                body.set('client_id', CLIENT_ID)
                body.set('code', code)
                body.set('redirect_uri', window.location.origin + '/')

                // PKCE: добавляем code_verifier, если есть
                const verifier = window.sessionStorage.getItem('pkce_verifier')
                if (verifier) {
                    body.set('code_verifier', verifier)
                }

                try {
                    const res = await fetch(TOKEN_URL, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                        },
                        body: body.toString(),
                    })

                    if (!res.ok) {
                        console.error('Token endpoint error, status =', res.status)
                        setAuthenticated(false)
                        setToken(null)
                    } else {
                        const data = await res.json()
                        console.log('TOKEN RESPONSE', data)

                        const accessToken = data.access_token
                        if (accessToken) {
                            setToken(accessToken)
                            setAuthenticated(true)
                            await fetchProducts(accessToken)
                        } else {
                            setAuthenticated(false)
                        }
                    }
                } catch (e) {
                    console.error('Token request failed:', e)
                    setAuthenticated(false)
                    setToken(null)
                } finally {
                    // чистим PKCE-данные и URL от code/state
                    window.sessionStorage.removeItem('pkce_verifier')
                    window.sessionStorage.removeItem('oauth_state')

                    window.history.replaceState(
                        null,
                        '',
                        window.location.origin + window.location.pathname,
                    )
                    setReady(true)
                }

                return
            }

            // если code нигде нет — пользователь ещё не логинился
            setAuthenticated(false)
            setToken(null)
            setReady(true)
        }

        doAuthFlow()
    }, [])

    // ===== Login / Logout =====

    const doLogin = async () => {
        const state = crypto.randomUUID()
        const redirectUri = encodeURIComponent(window.location.origin + '/')

        // генерируем PKCE-пару
        const { verifier, challenge } = await createPkcePair()

        // сохраняем в sessionStorage
        window.sessionStorage.setItem('pkce_verifier', verifier)
        window.sessionStorage.setItem('oauth_state', state)

        const url =
            `${AUTH_URL}?client_id=${encodeURIComponent(CLIENT_ID)}` +
            `&response_type=code` +
            `&scope=openid%20profile%20email` +
            `&redirect_uri=${redirectUri}` +
            `&state=${encodeURIComponent(state)}` +
            `&code_challenge=${encodeURIComponent(challenge)}` +
            `&code_challenge_method=S256`

        window.location.href = url
    }

    const doLogout = () => {
        const redirectUri = encodeURIComponent(window.location.origin + '/')
        const url =
            `${LOGOUT_URL}?client_id=${encodeURIComponent(
                CLIENT_ID,
            )}&post_logout_redirect_uri=${redirectUri}`

        window.location.href = url
    }

    // ===== UI =====

    if (!ready) {
        return (
            <div style={{ fontFamily: 'sans-serif', maxWidth: 900, margin: '30px auto' }}>
                <h1>Shop</h1>
                <p>Waiting for authentication...</p>
            </div>
        )
    }

    return (
        <div style={{ fontFamily: 'sans-serif', maxWidth: 900, margin: '30px auto' }}>
            <h1>Shop</h1>

            <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
                {authenticated && (
                    <button onClick={doLogout}>
                        Logout
                    </button>
                )}

                <button onClick={() => setTab('shop')}>Shop</button>

                {/* Админ-вкладку пока показываем вручную, роли можно прикрутить позже через декодирование JWT */}
                <button onClick={() => setTab('admin')}>Admin</button>
            </div>

            {!authenticated && (
                <div style={{ marginBottom: 10 }}>
                    <b>You are not logged in.</b>{' '}
                    <button onClick={doLogin}>Login</button>
                </div>
            )}

            {tab === 'shop' && (
                <>
                    <h2>Products</h2>
                    <table border="1" cellPadding="6">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Price</th>
                            <th>Qty</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {products.map((p) => (
                            <tr key={p.id}>
                                <td>{p.id}</td>
                                <td>{p.name}</td>
                                <td>{p.price}</td>
                                <td>{p.quantity}</td>
                                <td>
                                    <button
                                        onClick={() => orderOne(p.id)}
                                        disabled={!authenticated || !token || p.quantity < 1}
                                    >
                                        Order 1
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </>
            )}

            {tab === 'admin' && authenticated && (
                <Admin token={token} />
            )}
        </div>
    )
}
