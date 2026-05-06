import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Circle, Marker, Popup, useMapEvents } from 'react-leaflet';
import { icon } from 'leaflet';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import { safeZonesApi } from '../api/safezones';
import { patientsApi  } from '../api/patients';
import type { SafeZoneResponse, PatientDetailResponse } from '../types';
import { ArrowLeft, Plus, Trash2, MapPin } from 'lucide-react';
import 'leaflet/dist/leaflet.css';

const defaultIcon = icon({
  iconUrl:   'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize:   [25, 41],
  iconAnchor: [12, 41],
});

const newZoneIcon = icon({
  iconUrl:   'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize:   [25, 41],
  iconAnchor: [12, 41],
});

// Componente que captura clics en el mapa
const MapClickHandler = ({
  onMapClick,
  enabled,
}: {
  onMapClick: (lat: number, lon: number) => void;
  enabled:    boolean;
}) => {
  useMapEvents({
    click: (e) => {
      if (enabled) onMapClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
};

const SEVILLA: [number, number] = [37.3891, -5.9845];

const SafeZonePage = () => {
  const { patientId } = useParams<{ patientId: string }>();
  const navigate      = useNavigate();

  const [patient,   setPatient]   = useState<PatientDetailResponse | null>(null);
  const [zones,     setZones]     = useState<SafeZoneResponse[]>([]);
  const [loading,   setLoading]   = useState(true);
  const [saving,    setSaving]    = useState(false);
  const [deleting,  setDeleting]  = useState<string | null>(null);
  const [error,     setError]     = useState('');

  // Estado de la nueva zona que se está creando
  const [newZone, setNewZone] = useState<{
    latitude:     number;
    longitude:    number;
    radiusMeters: number;
  } | null>(null);

  const [isPlacing, setIsPlacing] = useState(false);

  useEffect(() => {
    if (!patientId) return;

    Promise.all([
      patientsApi.getById(patientId),
      safeZonesApi.getByPatient(patientId),
    ]).then(([{ data: p }, { data: z }]) => {
      setPatient(p);
      setZones(z);
    }).finally(() => setLoading(false));
  }, [patientId]);

  const handleMapClick = (lat: number, lon: number) => {
    setNewZone({
      latitude:     lat,
      longitude:    lon,
      radiusMeters: 200,
    });
    setIsPlacing(false);
  };

  const handleSaveZone = async () => {
    if (!newZone || !patientId) return;
    setSaving(true);
    setError('');
    try {
      const { data } = await safeZonesApi.create({
        patientId,
        ...newZone,
      });
      setZones(prev => [...prev, data]);
      setNewZone(null);
    } catch {
      setError('Error al guardar la zona segura');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteZone = async (zoneId: string) => {
    if (!confirm('¿Eliminar esta zona segura?')) return;
    setDeleting(zoneId);
    try {
      await safeZonesApi.delete(zoneId);
      setZones(prev => prev.filter(z => z.zoneId !== zoneId));
    } catch {
      setError('Error al eliminar la zona');
    } finally {
      setDeleting(null);
    }
  };

  const mapCenter: [number, number] = zones.length > 0
    ? [zones[0].latitude, zones[0].longitude]
    : SEVILLA;

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64 text-[#7F8C8D]">
          Cargando...
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      {/* Cabecera */}
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => navigate(`/patients/${patientId}`)}
          className="p-2 hover:bg-gray-100 rounded-lg transition-all"
        >
          <ArrowLeft size={20} className="text-[#2C3E50]" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-[#2C3E50]">
            Zonas seguras
          </h1>
          <p className="text-sm text-[#7F8C8D]">
            {patient?.fullName}
          </p>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg
                        px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* ── Columna izquierda: mapa ── */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                           tracking-wide">
              Mapa
            </h2>
            {!isPlacing && !newZone && (
              <button
                onClick={() => setIsPlacing(true)}
                className="flex items-center gap-2 text-sm text-[#1A8C7A]
                           hover:text-[#126B5E] font-medium transition-all"
              >
                <Plus size={16}
                      className="bg-[#1A8C7A] text-white rounded-full p-0.5" />
                Añadir zona segura
              </button>
            )}
            {isPlacing && (
              <p className="text-sm text-[#E67E22] animate-pulse">
                📍 Pulsa en el mapa para colocar la zona
              </p>
            )}
          </div>

          <MapContainer
            center={mapCenter}
            zoom={15}
            className="h-[420px] w-full rounded-2xl z-0"
          >
            <TileLayer
              attribution='&copy; <a href="https://openstreetmap.org">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            <MapClickHandler
              onMapClick={handleMapClick}
              enabled={isPlacing}
            />

            {/* Zonas existentes */}
            {zones.map((zone) => (
              <Circle
                key={zone.zoneId}
                center={[zone.latitude, zone.longitude]}
                radius={zone.radiusMeters}
                pathOptions={{
                  color:       '#1A8C7A',
                  fillColor:   '#1A8C7A',
                  fillOpacity: 0.15,
                  weight:      2,
                }}
              >
                <Popup>
                  <div className="text-sm">
                    <p className="font-medium">Zona segura</p>
                    <p className="text-gray-500">Radio: {zone.radiusMeters}m</p>
                  </div>
                </Popup>
              </Circle>
            ))}

            {/* Nueva zona en proceso */}
            {newZone && (
              <>
                <Marker
                  position={[newZone.latitude, newZone.longitude]}
                  icon={newZoneIcon}
                />
                <Circle
                  center={[newZone.latitude, newZone.longitude]}
                  radius={newZone.radiusMeters}
                  pathOptions={{
                    color:       '#CC2222',
                    fillColor:   '#CC2222',
                    fillOpacity: 0.15,
                    weight:      2,
                    dashArray:   '6 4',
                  }}
                />
              </>
            )}
          </MapContainer>
        </div>

        {/* ── Columna derecha: opciones y lista ── */}
        <div className="space-y-6">

          {/* Panel de nueva zona */}
          {newZone && (
            <div className="bg-white border border-[#1A8C7A] rounded-2xl p-6">
              <h3 className="font-semibold text-[#2C3E50] mb-4">
                Nueva zona segura
              </h3>

              <div className="space-y-4">
                <div>
                  <p className="text-xs text-[#7F8C8D] mb-1">Coordenadas</p>
                  <p className="text-sm font-mono text-[#2C3E50]">
                    {newZone.latitude.toFixed(5)},  {newZone.longitude.toFixed(5)}
                  </p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700 block mb-2">
                    Radio de seguridad:{' '}
                    <span className="text-[#1A8C7A] font-bold">
                      {newZone.radiusMeters}m
                    </span>
                  </label>
                  <input
                    type="range"
                    min={50}
                    max={2000}
                    step={50}
                    value={newZone.radiusMeters}
                    onChange={(e) => setNewZone({
                      ...newZone,
                      radiusMeters: Number(e.target.value),
                    })}
                    className="w-full accent-[#1A8C7A]"
                  />
                  <div className="flex justify-between text-xs text-[#7F8C8D] mt-1">
                    <span>50m</span>
                    <span>2000m</span>
                  </div>
                </div>

                <div className="flex gap-2 pt-2">
                  <Button
                    loading={saving}
                    onClick={handleSaveZone}
                    className="flex-1"
                  >
                    Guardar zona
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => setNewZone(null)}
                  >
                    Cancelar
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Lista de zonas existentes */}
          <div>
            <h3 className="text-sm font-semibold text-[#7F8C8D] uppercase
                           tracking-wide mb-3">
              Zonas configuradas ({zones.length})
            </h3>

            {zones.length === 0 ? (
              <div className="bg-white border border-gray-200 rounded-2xl p-8
                              text-center">
                <MapPin size={32} className="text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-[#7F8C8D]">
                  No hay zonas seguras configuradas
                </p>
                <p className="text-xs text-[#7F8C8D] mt-1">
                  Pulsa "Añadir zona segura" y haz clic en el mapa
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {zones.map((zone, index) => (
                  <div
                    key={zone.zoneId}
                    className="bg-white border border-gray-200 rounded-xl p-4
                               flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-[#1A8C7A]/10
                                      flex items-center justify-center">
                        <MapPin size={14} className="text-[#1A8C7A]" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-[#2C3E50]">
                          Zona {index + 1}
                        </p>
                        <p className="text-xs text-[#7F8C8D]">
                          Radio: {zone.radiusMeters}m ·{' '}
                          {zone.latitude.toFixed(4)}, {zone.longitude.toFixed(4)}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDeleteZone(zone.zoneId)}
                      disabled={deleting === zone.zoneId}
                      className="p-2 text-[#7F8C8D] hover:text-[#CC2222]
                                 hover:bg-red-50 rounded-lg transition-all
                                 disabled:opacity-40"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default SafeZonePage;