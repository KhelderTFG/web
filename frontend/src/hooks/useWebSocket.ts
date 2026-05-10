import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { AlertNotification, LocationNotification, VitalsNotification } from '../types';

interface UseWebSocketOptions {
  caregiverId: string;
  onAlert:     (alert: AlertNotification) => void;
  onVitals?:   (vitals: VitalsNotification) => void;
  onLocation?: (location: LocationNotification) => void;
  enabled:     boolean;
}

export const useWebSocket = ({
  caregiverId,
  onAlert,
  onVitals,
  onLocation,
  enabled,
}: UseWebSocketOptions) => {
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay:   5000,
      onConnect: () => {
        console.log('WebSocket conectado');

        stompClient.subscribe(`/topic/alerts/${caregiverId}`, (message) => {
          const raw = JSON.parse(message.body);
          const alert: AlertNotification = {
            alertId:      raw.alert_id,
            deviceId:     raw.device_id,
            patientId:    raw.patient_id,
            patientName:  raw.patient_name,
            alertType:    raw.alert_type,
            status:       raw.status,
            latitude:     raw.latitude,
            longitude:    raw.longitude,
            heartRate:    raw.heart_rate,
            batteryLevel: raw.battery_level,
            timestamp:    raw.timestamp,
          };
          onAlert(alert);
        });

        stompClient.subscribe(`/topic/location/${caregiverId}`, (message) => {
            const raw = JSON.parse(message.body);
            onLocation?.({
                deviceId:  raw.device_id,
                patientId: raw.patient_id,
                latitude:  raw.latitude,
                longitude: raw.longitude,
            });
        });

        stompClient.subscribe(`/topic/vitals/${caregiverId}`, (message) => {
          const raw = JSON.parse(message.body);
          onVitals?.({
            deviceId:  raw.device_id,
            patientId: raw.patient_id,
            heartRate: raw.heart_rate,
            spO2:      raw.spo2,
            steps:     raw.steps,
            timestamp: raw.timestamp,
          });
        });

        stompClient.subscribe('/topic/connection', (message) => {
          const status = JSON.parse(message.body);
          console.log('Estado conexión:', status);
        });
      },
      onDisconnect: () => {
        console.log('WebSocket desconectado');
      },
    });

    stompClient.activate();
    clientRef.current = stompClient;
  }, [caregiverId, onAlert, onVitals, onLocation]);

  useEffect(() => {
    if (!enabled) return;
    connect();
    return () => {
      clientRef.current?.deactivate();
    };
  }, [enabled, connect]);
};