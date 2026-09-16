# Changelog

## 0.8.0 — Gimbal Controller

**Added**
- **Gimbal Controller**: keeps a Create Aeronautics vehicle from falling over sideways and still lets it lean into turns. Redstone switches it off. Needs Sable (comes with Aeronautics).

**Changed**
- Transmission reworked into a gearshift: each of its four gears is a set output speed (32, 64, 128 and 256 RPM by default), whatever the input.
- CC: Tweaked: `transmission` peripheral methods changed (gears are numbers 1–4; new `getDirection`, `setDirection`, `getGearSpeeds`, `setGearSpeed`).

## 0.7.0 — Transmission remodel

**Changed**
- New Transmission model: iron case with brass flanges, and a gear cluster in a channel along one edge.
- The gears show the gear: a sliding gear moves to the selected one and turns with the output, hides in Neutral, and the brass gear at the input end is Reverse.
- The role marks are cast brass relief.
- Shaft ends stand a pixel proud of the bearing plates.

**Removed**
- The gear wheels on each face.

## 0.6.0 — Create: Vehicle Surplus

Create: Fuel Tank is now **Create: Vehicle Surplus**, a home for vehicle parts in general.

**Added**
- **Transmission**: an inline gearbox with Reverse, Neutral and gears 1/4, 1/2, 3/4 and 1:1.
- Built-in Redstone Link slots on the Transmission's faces: Up and Down shift a gear, Analog picks the gear by signal strength, Neutral locks the output. Wired redstone works too.
- Gear wheels on the Transmission show the current gear; Goggles show the gear and both speeds.
- Optional CC: Tweaked support: the Transmission is a `transmission` peripheral.
- **Differential**: a Gearbox whose outputs all turn the input's way, for driven axles.
- Ponder scenes for every block.

**Changed**
- New name and mod id: `createvehiclesurplus` (was `createfueltank`). Blocks placed with an older version do not carry over, so break them before updating.

## 0.3.0 — Long Fuel Tank

**Added**
- **Long Fuel Tank**: the Fuel Tank lying down, a 1x1 tube up to 8 blocks long.
- Recipe: a Fuel Tank alone in the grid (and back).

## 0.2.0 — Create Propulsion support

**Added**
- Fuel Tanks feed Create Propulsion: Simulated's Thruster and Liquid Vector Thruster (optional).

**Changed**
- Goggles count machines (engines and thrusters) instead of engines.

## 0.1.0 — Initial release

**Added**
- **Fuel Tank**: a Create Fluid Tank that feeds every Diesel Generators engine touching it, on any side.
- Only fluids the engine burns are fed; redstone on any block of the tank stops the feed.
- Multiblocks up to 3x3, like the Fluid Tank.
- Goggles show the feed status.
- Recipe: Brass Sheet, Wooden Barrel, Brass Sheet in a column.
