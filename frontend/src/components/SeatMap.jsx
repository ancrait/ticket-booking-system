const SECTOR_COLORS = {
    VIP: '#f59e0b',
    A: '#3b82f6',
    B: '#10b981',
    C: '#8b5cf6',
    D: '#ec4899',
    GOOD: '#93c5fd',
    'SUPER LUX': '#ef4444',
    DEFAULT: '#94a3b8',
};

function getSectorColor(sector) {
    return SECTOR_COLORS[sector?.toUpperCase()] || SECTOR_COLORS.DEFAULT;
}

export default function SeatMap({ seats, selectedSeats, onToggle }) {
    const selectedIds = new Set(selectedSeats.map((s) => s.id));

    const rowsMap = seats.reduce((acc, seat) => {
        if (!acc[seat.rowNumber]) {
            acc[seat.rowNumber] = [];
        }
        acc[seat.rowNumber].push(seat);
        return acc;
    }, {});

    const rows = Object.entries(rowsMap)
        .map(([rowNumber, rowSeats]) => ({
            rowNumber: Number(rowNumber),
            seats: rowSeats.sort((a, b) => a.seatNumber - b.seatNumber),
        }))
        .sort((a, b) => a.rowNumber - b.rowNumber);

    const sectorPrices = seats.reduce((acc, seat) => {
        if (!acc[seat.sector]) {
            acc[seat.sector] = seat.price;
        }
        return acc;
    }, {});

    const legend = Object.entries(sectorPrices).map(([sector, price]) => ({
        sector,
        price,
        color: getSectorColor(sector),
    }));

    return (
        <div className="seat-map">
            <div className="legend">
                {legend.map(({ sector, price, color }) => (
                    <div key={sector} className="legend-item">
                        <span
                            className="legend-color"
                            style={{ backgroundColor: color }}
                        />
                        <span className="legend-text">
                            {sector} — {Number(price).toFixed(0)} UAH
                        </span>
                    </div>
                ))}
            </div>

            <div className="screen-container">
                <div className="screen-line" />
                <div className="screen-label">ЕКРАН</div>
            </div>

            <div className="rows">
                {rows.map(({ rowNumber, seats: rowSeats }) => (
                    <div key={rowNumber} className="seat-row">
                        <span className="row-label">{rowNumber}</span>
                        <div className="seat-cells">
                            {rowSeats.map((seat) => {
                                const isSelected = selectedIds.has(seat.id);
                                const isAvailable = seat.status === 'FREE';
                                const color = getSectorColor(seat.sector);

                                return (
                                    <button
                                        key={seat.id}
                                        type="button"
                                        className={`seat-cell ${isSelected ? 'selected' : ''} ${!isAvailable ? 'unavailable' : ''}`}
                                        style={
                                            isAvailable && !isSelected
                                                ? { backgroundColor: color, borderColor: color }
                                                : undefined
                                        }
                                        onClick={() => isAvailable && onToggle(seat)}
                                        disabled={!isAvailable}
                                        title={`Ряд ${seat.rowNumber}, Місце ${seat.seatNumber}, ${seat.sector}, ${seat.price} UAH`}
                                        aria-label={`Ряд ${seat.rowNumber}, Місце ${seat.seatNumber}, ${seat.sector}, ${seat.price} UAH`}
                                    >
                                        {seat.seatNumber}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
