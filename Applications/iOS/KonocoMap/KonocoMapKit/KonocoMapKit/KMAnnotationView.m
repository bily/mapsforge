//
//  KMAnnotationView.m
//  KonocoMapKit
//
//  Created by Tobias Kräntzer on 03.06.11.
//  Copyright 2010, 2011 Konoco <http://konoco.org/> All rights reserved.
//
//  This file is part of KonocoMapKit.
//	
//  Map is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//	
//  Map is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License
//  along with Map.  If not, see <http://www.gnu.org/licenses/>.
//

#import "KMAnnotationView.h"


@implementation KMAnnotationView

@synthesize annotation = _annotation;

- (id)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
    }
    
    return self;
}

- (void)dealloc
{
    [_annotation release];
    [super dealloc];
}

- (void)drawRect:(NSRect)dirtyRect
{
    [[NSColor grayColor] setFill];
    [[NSColor whiteColor] setStroke];
    
    // Drawing code here.
    NSBezierPath *path = [NSBezierPath bezierPathWithOvalInRect:self.bounds];
    [path fill];
    [path stroke];
}

@end
