/*----------------------------------------------------------------------------*
 *
 * Basic Robot Code Compiled By Blake from Team 2648
 *
 * Questions? blake (at) team2648 [dot] com
 *
/*----------------------------------------------------------------------------*/
package edu.wpi.first.wpilibj.templates;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStationLCD;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SimpleRobot;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.Timer;


/* Imports are not needed when the package is edu.wpi.first.wpilibj; contrary to that of the basic netbeans frc project
import edu.wpi.first.wpilibj.RobotDrive;*/
public class SimpleSoccer extends SimpleRobot
{

    private Jaguar Rdrive, Ldrive;
    private RobotDrive drivetrain;
    private Joystick LS; //One Joystick controls the robot
    private Joystick RS; //controls camera
    private Victor winch; // The winch that controls the lift
    private Jaguar roller; // Controls the soccer balls to placement in front of kicker
    private Victor kicker; // The kicker
    private Victor quickRelease; // The motor that releases the arm
    private DigitalInput liftDwn;
    private DigitalInput kickerReady;
    private DigitalInput switch1;
    private DigitalInput switch2;
    private DigitalInput haveBall;
    private Gyro gSensor;
    private DriverStationLCD dsLCD;
    private DriverStation ds;
    private int count, zone;
    private String str1, str2, str3;
    private boolean driveCount = true;
    private Timer time;
    private int slot = 4;

    public SimpleSoccer()
    {

	getWatchdog().setEnabled(false); // Stupid WATCHDOG -- Doubtfull that this is legal
	dsLCD = DriverStationLCD.getInstance();
	ds = DriverStation.getInstance(); //We use this to set lights on our HUD and get info from switches

	Rdrive = new Jaguar(slot, 1);
	Ldrive = new Jaguar(slot, 2);
	drivetrain = new RobotDrive(Rdrive, Ldrive); // create RobotDrive

	// joysticks
	LS = new Joystick(1); // Driving during teleop
	RS = new Joystick(2); // Driving during finale

	liftDwn = new DigitalInput(slot, 14); //Simple microswitch
	kickerReady = new DigitalInput(slot, 13); //Simple microswitch (Is the kicker cocked?)
	switch1 = new DigitalInput(slot, 1); //Simple microswitch for auto code
	switch2 = new DigitalInput(slot, 2); //Simple microswitch for auto code
	haveBall = new DigitalInput(slot, 5); // Optical sensor - in ball caputre device

	winch = new Victor(slot, 3); // and the winch to lift the robot in finale
	kicker = new Victor(slot, 5); // and the kicker to kick the ball
	roller = new Jaguar(slot, 4); // and the roller to hold the ball
	quickRelease = new Victor(slot, 6); // and the Quick release to remove the arm tiedown

	gSensor = new Gyro(1);

	zone = 2; //Default beginning zone if auto switches are missing
	time = new Timer(); // To allow for the calculation of when to iluminate the light on the HUD
    }

    public void autonomous()
    {
	dsLCD.updateLCD();
	getWatchdog().setEnabled(false); // Stupid WATCHDOG
	count = 0;  // represents number of kicks
	zone = 2;

	while (isAutonomous())
	{
	    if (switch1.get() && switch2.get())
		zone = 1;
	    else if (switch1.get() && !switch2.get())
		zone = 2;
	    else if (!switch1.get() && !switch2.get())
		zone = 3;
	    else
		zone = 2;

	    // This once was to print code to the DS screen
	    //dsLCD.println(DriverStationLCD.Line.kMain6, 1, "");

	    //This activates the HUD light for when we have captured a ball
	    ds.setDigitalOut(1, !haveBall.get());

	    //Some information about the auto code running.
	    str1 = "count: " + count;
	    str2 = "zone: " + zone;
	    dsprint(str1, 1);
	    dsprint(str2, 15);

	    //// Psuedo code
	    //=>One ball
	    //Driveforeward, kick, turn, backup
	    //=>Two Balls
	    // fwd, kick, fwd, kick, turn, backup
	    //=>Three Balls
	    // fwd, kick, fwd, kick, fwd, kick, turn, backup
	    str3 = "Current Gyro: " + gSensor.getAngle(); // Remember it zeros at startup
	    dsLCD.println(DriverStationLCD.Line.kUser4, 1, str3);

	    reloadKicker();
	    if (count < zone)
		if (!haveBall.get())
		{
		    drivetrain.setLeftRightMotorOutputs(0, 0);
		    if (kickerReady.get())
		    {
			kick();
			Timer.delay(1);
			count++;
		    }
		} else
		    drivetrain.setLeftRightMotorOutputs(-.4, -.4);
	    else
	    {
		str3 = "Current Gyro: " + gSensor.getAngle();
		dsLCD.println(DriverStationLCD.Line.kUser4, 1, str3);
		// Former gyro turn code
//                if (gSensor.getAngle() > -100 && gSensor.getAngle() < 10) {
//                    drivetrain.setLeftRightMotorSpeeds(.75, -.75);
////                    reloadKicker();
//                }

		reloadKicker();
		if (kickerReady.get())
		    return;
		else if (driveCount)
		{
		    drivetrain.setLeftRightMotorOutputs(.5, .5);
		    Timer.delay(.5);
		    drivetrain.setLeftRightMotorOutputs(0, 0);
		    Timer.delay(.25);
		    stop();
		    driveCount = false;
		}
	    }
	}
    }

    public void operatorControl()
    {
	getWatchdog().setEnabled(false); // Stupid WATCHDOG
	time.reset();
	time.start(); // For the HUD light

	while (isOperatorControl()) //Let's loop while in operator control
	{
	    reloadKicker();
//=============================================================================
	    // Kicker Code
	    if (LS.getTrigger())
		kick();
	    reloadKicker();

//=============================================================================
	    // Winch Code
	    if (liftDwn.get()) // if the winch is down, only let it go up.
		winchOnlyUp();
	    else
		winchUpAndDown();
//=============================================================================
	    //Drive Code
	    if (liftDwn.get()) // In the finale we switch drivers
		drivetrain.arcadeDrive(LS);
	    else
		drivetrain.arcadeDrive(RS);
//=============================================================================
	    //(HUD) heads up display code
	    ds.setDigitalOut(1, !haveBall.get());

//=============================================================================

	    //Printout the value of the timer. @todo make this match teh field. Is there a method?
	    dsLCD.println(DriverStationLCD.Line.kUser4, 1, new Double(time.get()).toString());

	    dsLCD.updateLCD();

	    // If the timer is greater than 84 sec light up the hud.
	    if (time.get() > 85)
		ds.setDigitalOut(2, true);
	    else
		ds.setDigitalOut(2, false);
	}
	stop();
    }

    public void kick()
    {
	if (liftDwn.get())
	{
	    kicker.set(1);
	    //==================
	    // This will allow for roller back speed
	    // aka kicker distance
	    //roller.set(-1);
	    Timer.delay(.05);
	    
	    // @todo disable kicking when travling over bump!!!!
	} else
	    reloadKicker();
    }

     // sets all motors to spped zero
    public void stop()
    {  
	kicker.set(0);
	roller.set(0);
	drivetrain.drive(0, 0);
	winch.set(0);
    }

    public void reloadKicker()
    {   // Automatically recock the kicker
	if (!kickerReady.get())
	    kicker.set(0.75);
	else
	{
	    kicker.set(0);
	    if (liftDwn.get())
		roller.set(-.61);
	    else
		roller.set(0);
	}
    }

    // Only allow the arm to move up
    private void winchOnlyUp()
    {   
	if (RS.getRawButton(3))
	    quickRelease.set(-1);
	else if (RS.getRawButton(7))
	    winch.set(1);
	else
	{
	    quickRelease.set(0);
	    winch.set(0);
	}
    }

    private void winchUpAndDown()
    {
	if (RS.getRawButton(2) || RS.getRawButton(1))
	    winch.set(-1);
	else if (RS.getRawButton(3))
	    quickRelease.set(-1);
	else if (RS.getRawButton(7))
	    winch.set(1);
	else
	{
	    quickRelease.set(0);
	    winch.set(0);
	}
    }

    private void dsprint(String str, int col)
    {
	dsLCD.println(DriverStationLCD.Line.kMain6, col, str);
	dsLCD.updateLCD();
    }
}
/*----------------------------------------------------------------------------*
 *
 * Basic Robot Code Compiled By Blake from Team 2648
 *
 * Questions? blake (at) team2648 [dot] com
 *
/*----------------------------------------------------------------------------*/