/*
 * Copyright (C) 2015 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package epiinf;

/**
 * Class representing model events such as rate changes and
 * contemporaneous sampling events.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class ModelEvent extends Event {
    public enum Type { RHO_SAMPLING, RATE_CHANGE};
    public Type type;

    public enum RateVariableType { INFECTION_RATE, RECOVERY_RATE, PSI_SAMPLING_VARIABLE, REMOVAL_PROB}
    public RateVariableType rateVariableType;

    public double newRateVariableValue;
    public double rho;
}
