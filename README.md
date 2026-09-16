# Create: Vehicle Surplus

<p align="center"><img src="thumbnail.png" width="256" alt="Create: Vehicle Surplus"></p>

Compact vehicle parts for [Create](https://modrinth.com/mod/create) on NeoForge 1.21.1. Made for
Create Aeronautics / Simulated builds, and they work anywhere else too.

## Blocks

- **Fuel Tank**: a Create Fluid Tank that feeds every Diesel Generators engine touching it. No
  pumps or pipes, and only fluids the engine burns go in. Redstone on any block of the tank
  stops the feed.
- **Long Fuel Tank**: the same tank lying down, a 1x1 tube up to 8 blocks long.
- **Differential**: a Gearbox whose outputs all turn the input's way, for driven axles.
- **Transmission**: an inline gearshift. Its four gears are set output speeds (32, 64, 128 and
  256 RPM by default, shift-right-click to change them), whatever the input. Each long face has
  built-in Redstone Link slots: hold Forward or Reverse to drive, pulse Up or Down to shift.
- **Gimbal Controller**: keeps an Aeronautics vehicle from falling over sideways and still lets it
  lean into turns, so two-wheelers work. Place it inline on the driveshaft with its shaft along the
  vehicle's forward axis. Heavier vehicles need more than one. Redstone switches it off.

Every block has a Ponder scene.

## Compatibility

- **Create Propulsion: Simulated** (optional): Fuel Tanks also feed its liquid thrusters.
- **Create Aeronautics / Simulated** (optional): the Gimbal Controller balances its vehicles.
- **CC: Tweaked** (optional): the Transmission is a `transmission` peripheral with `getGear`,
  `setGear`, `shiftUp`, `shiftDown`, `getDirection`, `setDirection`, `getGearSpeeds`,
  `setGearSpeed`, `getInputSpeed` and `getOutputSpeed`.

## Requirements

| Mod | Version |
|---|---|
| NeoForge | 21.1.219+ |
| Create | 6.0.10 – 6.0.x |
| Create Diesel Generators | 1.21.1-1.3.15+ |
| Create Propulsion: Simulated | 1.1.5+ (optional) |
| Sable | 2.0.5+ (optional, for the Gimbal Controller; bundled with Create Aeronautics) |
| CC: Tweaked | 1.113+ (optional) |
