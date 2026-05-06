import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { AlertNotification } from '../types';

interface UseWebSocketOptions {
  caregiverId: string;
  onAlert:     (alert: AlertNotification) => void;
  enabled:     boolean;
}

export const useWebSocket = ({
  caregiverId,
  onAlert,
  enabled,
}: UseWebSocketOptions) => {
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay:   5000,
      onConnect: () => {
        console.log('WebSocket conectado');

        // Suscribirse al topic general
        stompClient.subscribe('/topic/alerts', (message) => {
          const alert: AlertNotification = JSON.parse(message.body);
          onAlert(alert);
        });

        // Suscribirse al topic específico del cuidador
        stompClient.subscribe(
          `/topic/alerts/${caregiverId}`,
          (message) => {
            const alert: AlertNotification = JSON.parse(message.body);
            onAlert(alert);
          }
        );
      },
      onDisconnect: () => {
        console.log('WebSocket desconectado');
      },
    });

    stompClient.activate();
    clientRef.current = stompClient;
  }, [caregiverId, onAlert]);

  useEffect(() => {
    if (!enabled) return;
    connect();
    return () => {
      clientRef.current?.deactivate();
    };
  }, [enabled, connect]);
};