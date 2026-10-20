/**
 * @deprecated Remplacé par jwtInterceptor et errorInterceptor dans app.config.ts
 */
export { jwtInterceptor, jwtInterceptor as authInterceptor } from './jwt.interceptor';
export { errorInterceptor } from './error.interceptor';
