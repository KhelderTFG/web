import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import { icon } from 'leaflet';
import type { SafeZoneResponse } from '../../types';
import 'leaflet/dist/leaflet.css';

// Fix icono por defecto de Leaflet con Vite
const defaultIcon = icon({
  iconUrl:    'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:  'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize:   [25, 41],
  iconAnchor: [12, 41],
});

interface Props {
  latitude:   number | null;
  longitude:  number | null;
  safeZones:  SafeZoneResponse[];
  patientName: string;
}

const SEVILLA_CENTER: [number, number] = [37.3891, -5.9845];

const PatientMap = ({ latitude, longitude, safeZones, patientName }: Props) => {
  const position: [number, number] = latitude && longitude
    ? [latitude, longitude]
    : SEVILLA_CENTER;

  return (
    <MapContainer
      center={position}
      zoom={15}
      className="h-72 w-full rounded-xl z-0"
    >
      <TileLayer
        attribution='&copy; <a href="https://openstreetmap.org">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Marcador de ubicación actual */}
      {latitude && longitude && (
        <Marker position={[latitude, longitude]} icon={defaultIcon}>
          <Popup>{patientName}</Popup>
        </Marker>
      )}

      {/* Zonas seguras */}
      {safeZones.map((zone) => (
        <Circle
          key={zone.zoneId}
          center={[zone.latitude, zone.longitude]}
          radius={zone.radiusMeters}
          pathOptions={{
            color:       '#1A8C7A',
            fillColor:   '#1A8C7A',
            fillOpacity: 0.15,
          }}
        />
      ))}
    </MapContainer>
  );
};

export default PatientMap;